package searchengine.services.page;

import org.springframework.stereotype.Service;
import searchengine.model.PageRepository;
import searchengine.model.domain.SiteDto;
import searchengine.model.entity.PageEntity;

import java.util.Optional;

@Service
public class PageServiceImpl implements PageService{

    private final PageRepository pageRepository;

    public PageServiceImpl(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Override
    public Optional<PageEntity> findPageEntityByPathAndSiteId(String path, int siteId) {
        return pageRepository.findPageEntityByPathAndSiteId(path, siteId);
    }

    @Override
    public PageEntity save(PageEntity page) {
        return pageRepository.save(page);
    }

    @Override
    public void deleteByPathAndSiteId(String path, int id) {
        pageRepository.deleteByPathAndSiteId(path, id);
    }

    @Override
    public void createNewPage(String url, String content, int statusCode, SiteDto dto) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPath(url);
        pageEntity.setCode(statusCode);
        pageEntity.setContent(content);
        pageEntity.setSiteId(dto.getId());
        pageRepository.save(pageEntity);
    }
}
