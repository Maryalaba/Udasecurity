module com.udacity.catpoint.securityService {
    requires java.desktop;
    requires com.udacity.catpoint.imageService;
    requires com.google.gson;
    requires com.google.common;
    requires java.prefs;
    requires miglayout;
    exports com.udacity.catpoint.securityService.application;
    exports com.udacity.catpoint.securityService.data;
    exports com.udacity.catpoint.securityService.service;
}