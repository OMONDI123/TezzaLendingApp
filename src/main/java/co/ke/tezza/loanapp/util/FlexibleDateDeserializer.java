package co.ke.tezza.loanapp.util;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class FlexibleDateDeserializer extends JsonDeserializer<Date> {
    private static final List<String> PATTERNS = Arrays.asList(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    );

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateStr = p.getText();
        for (String pattern : PATTERNS) {
            try {
                return new SimpleDateFormat(pattern).parse(dateStr);
            } catch (ParseException e) { /* continue */ }
        }
        throw new JsonParseException(p, "Unable to parse date: " + dateStr);
    }
}
