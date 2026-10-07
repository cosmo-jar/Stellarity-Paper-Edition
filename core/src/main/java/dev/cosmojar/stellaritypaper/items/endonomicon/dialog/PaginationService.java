package dev.cosmojar.stellaritypaper.items.endonomicon.dialog;

import java.util.List;

public final class PaginationService {

    public static <T> List<T> getPage(List<T> items, int page, int pageSize) {
        if (items == null || items.isEmpty()) return List.of();
        int totalPages = getTotalPages(items.size(), pageSize);
        int targetPage = Math.min(Math.max(1, page), totalPages);

        int fromIndex = (targetPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, items.size());

        if (fromIndex >= items.size()) return List.of();
        return items.subList(fromIndex, toIndex);
    }

    public static int getTotalPages(int totalItems, int pageSize) {
        if (totalItems <= 0 || pageSize <= 0) return 1;
        return (int) Math.ceil((double) totalItems / pageSize);
    }
}
