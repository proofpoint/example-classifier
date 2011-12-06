package com.proofpoint.classifier;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Path("/v1/classify")
public class ClassifierResource
{
    private final Pattern CREDIT_CARD_PATTERN = Pattern.compile("(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11})");
    private final Pattern SSN_PATTERN = Pattern.compile("\\d{3}-\\d{2}-\\d{4}");

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Number> post(InputStream input)
            throws IOException
    {
        CharsetDecoder decoder = Charsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        decoder.replaceWith("\uFFFD");

        InputStreamReader reader = new InputStreamReader(input, decoder);
        String text = CharStreams.toString(reader);

        ImmutableMap.Builder<String, Number> builder = ImmutableMap.builder();

        if (CREDIT_CARD_PATTERN.matcher(text.replaceAll("[ -]+", "")).find()) {
            builder.put("CreditCard", 100);
        }

        if (SSN_PATTERN.matcher(text).find()) {
            builder.put("SSN", 100);
        }

        return builder.build();
    }
}
