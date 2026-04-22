package com.bizcord.backend.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class HtmlEscapeMapper {
    @Named("escapeHtml")
    public String escapeHtml(String value) {
        return value == null ? null : HtmlUtils.htmlEscape(value);
    }
}
