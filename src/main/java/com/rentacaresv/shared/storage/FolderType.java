package com.rentacaresv.shared.storage;

/**
 * Enum para los tipos de carpetas en Digital Ocean Spaces
 */
public enum FolderType {
    PROFILE_PHOTOS("profile_photos"),
    CARS("cars"),
    CAR_DETAILS("car_details"),
    LOGOS("logos"),
    SETTINGS("settings");

    private final String folderName;

    FolderType(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}
