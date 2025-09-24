package io.github.lvoxx.srms.common.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageUtils {
    private final MessageSource messageSource;

    public MessageUtils(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String key, Object[] args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}