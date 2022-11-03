module com.udacity.catpoint.imageService {
    requires org.slf4j;
    requires software.amazon.awssdk.services.rekognition;
    requires java.desktop;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.regions;
    exports com.udacity.catpoint.imageService;
}