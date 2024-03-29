package searchengine.services.data_manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.domain.PageDto;
import searchengine.model.domain.PageDtoMapper;
import searchengine.model.domain.SiteDto;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.PageEntity;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.services.lemmatizer.IndexService;
import searchengine.services.lemmatizer.LemmaFinder;
import searchengine.services.lemmatizer.LemmaService;
import searchengine.services.my_assistant.MyConnector;
import searchengine.services.page.PageService;
import searchengine.services.page_parser.PageValidator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DataManagerServiceImpl implements DataManagerService {

    private static final Log log = LogFactory.getLog(IndexingServiceImpl.class);

    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;

    @Autowired
    public DataManagerServiceImpl(PageService pageService, LemmaService lemmaService, IndexService indexService) {
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
    }

    @Override
    public void insertDataPageToDB(SiteDto siteDto, String pageUrl, int statusCode, String content, String text) {
        String path = PageValidator.getPathFromUrl(pageUrl);
        Optional<PageEntity> optionalPage = pageService.findPageEntityByPathAndSiteId(path, siteDto.getId());
        if (optionalPage.isEmpty()) {
            PageEntity page = new PageEntity();
            page.setPath(path);
            page.setSiteId(siteDto.getId());
            page.setContent(content);
            page.setCode(statusCode);
            pageService.save(page);
            log.info("Добавили путь: " + path + " Сайта: " + siteDto.getName());
            insertLemmaAndIndexToDB(siteDto, PageDtoMapper.toDomain(page), text);
        }
    }

    @Override
    public void insertBadUrlToDB(SiteDto dto, String path, int statusCode) {
        Optional<PageEntity> optionalPage = pageService.findPageEntityByPathAndSiteId(path, dto.getId());
        if (optionalPage.isEmpty()) {
            PageEntity page = new PageEntity();
            page.setPath(path);
            page.setSiteId(dto.getId());
            page.setCode(statusCode);
            page.setContent("-");
            pageService.save(page);
            log.info("Добавили путь: " + path + " Сайта: " + dto.getName() + statusCode);
        }
    }

    @Override
    public void insertNewPage(String url, SiteDto siteDto) {
        try {
            var response = MyConnector.getResponse(url, siteDto);
            Document document = response.parse();
            String content = document.outerHtml();
            int statusCode = response.statusCode();
            String text = document.text();
            PageEntity pageEntity = new PageEntity();
            pageService.createNewPage(url, content, statusCode, siteDto);
            insertLemmaAndIndexToDB(siteDto, PageDtoMapper.toDomain(pageEntity), text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertLemmaAndIndexToDB(SiteDto siteDto, PageDto pageDto, String text) {
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(text);
            lemmaMap.forEach((key, value) -> {
                LemmaEntity lemmaEntity = new LemmaEntity();
                IndexEntity indexEntity = new IndexEntity();
                Optional<LemmaEntity> optionalLemma = lemmaService.findByLemma(key);
                if (optionalLemma.isEmpty()) {
                    lemmaEntity.setLemma(key);
                    lemmaEntity.setFrequency(1);
                } else {
                    lemmaEntity = optionalLemma.get();
                    lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                }
                lemmaEntity.setSiteId(siteDto.getId());
                lemmaService.save(lemmaEntity);
                indexEntity.setLemmaId(lemmaEntity.getId());
                indexEntity.setPageId(pageDto.getId());
                indexEntity.setRank(value);
                indexService.save(indexEntity);
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteLemmaAndIndexByPagePath(SiteDto siteDto, PageDto pageDto) {
        List<LemmaEntity> lemmas = lemmaService.findAllBySiteId(siteDto.getId());
        List<IndexEntity> indexes = indexService.findAllByPageId(pageDto.getId());
        pageService.deleteByPathAndSiteId(pageDto.getPath(), siteDto.getId());
        if (!indexes.isEmpty() || !lemmas.isEmpty()) {
            lemmaService.deleteAllBySiteId(siteDto.getId());
            indexService.deleteAllByPageId(pageDto.getId());
        }
    }
}
