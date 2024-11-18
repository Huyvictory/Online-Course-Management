package com.online.course.management.project.constants;

public class ChapterConstants {
    public static final String BASE_PATH = "/api/v1/chapters";
    public static final String CREATE_PATH = "/create";
    public static final String BULK_CREATE_PATH = "/bulk-create";
    public static final String UPDATE_DETAILS_PATH = "/{id}/update";
    public static final String BULK_UPDATE_PATH = "/bulk-update";
    public static final String DELETE_PATH = "{id}/delete";
    public static final String BULK_DELETE_PATH = "/bulk-delete";
    public static final String RESTORE_PATH = "{id}/restore";
    public static final String BULK_RESTORE_PATH = "/bulk-restore";
    public static final String GET_DETAILS_PATH = "/{id}/details";
    public static final String GET_DETAILS_WITH_LESSONS_PATH = "/{id}/details-with-lessons";
    public static final String GET_CHAPTERS_BY_COURSE_PATH = "{courseId}/list";
    public static final String SEARCH_CHAPTERS_PATH = "/search";
    public static final String CHAPTER_REORDERED_PATH = "/{id}/reorder";
}
