package searchengine.services.search;

import searchengine.dto.search_resp.SearchResponse;

public interface SearchService {
    SearchResponse search(String query, String site, int offset, int limit);
}
