package ua.ndmik.bot.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long chatId) {
        super("User not found for chatId=" + chatId);
    }
}
