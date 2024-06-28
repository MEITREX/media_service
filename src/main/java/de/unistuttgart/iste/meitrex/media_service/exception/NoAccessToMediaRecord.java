package de.unistuttgart.iste.meitrex.media_service.exception;

public class NoAccessToMediaRecord extends RuntimeException{

    public NoAccessToMediaRecord() {
        super("User does not have access to one or more courses.");
    }
}
