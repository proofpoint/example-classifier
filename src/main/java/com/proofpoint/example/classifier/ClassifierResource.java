package com.proofpoint.example.classifier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/v1/classify")
public class ClassifierResource
{
    private final Pattern CREDIT_CARD_PATTERN = Pattern.compile("4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11}");
    private final Pattern SSN_PATTERN = Pattern.compile("\\d{3}-\\d{2}-\\d{4}");
    private final StreamFlattener flattener = new StreamFlattener();

    private final UsersOnHold usersOnHold;

    @Inject
    public ClassifierResource(UsersOnHold usersOnHold)
    {
        this.usersOnHold = usersOnHold;
    }

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Number> post(@HeaderParam("X-NP-User-Name") String userId, final InputStream input)
            throws IOException
    {
        final Map<String, Number> result = Maps.newHashMap();

        if (usersOnHold.contains(userId)) {
            result.put("Hold", 100);
        }

        flattener.flatten(input, new EntryProcessor()
        {
            @Override
            public void process(InputStream entryStream)
                    throws IOException
            {
                CharsetDecoder decoder = Charsets.UTF_8.newDecoder();
                decoder.onMalformedInput(CodingErrorAction.REPLACE);
                decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
                decoder.replaceWith("\uFFFD");

                InputStreamReader reader = new InputStreamReader(entryStream, decoder);

                String text = CharStreams.toString(reader);

                Matcher creditCardMatcher = CREDIT_CARD_PATTERN.matcher(text.replaceAll("[ -]+", ""));
                while (creditCardMatcher.find() && isValidCreditCard(creditCardMatcher.group())) {
                    result.put("CreditCard", 100);
                }

                if (SSN_PATTERN.matcher(text).find()) {
                    result.put("SSN", 100);
                }
            }
        });

        return result;
    }

    @VisibleForTesting
    boolean isValidCreditCard(String number)
    {
        int sum = 0;
        boolean even = false;
        for (int i = number.length() - 1; i >= 0; --i) {
            int digit = Integer.parseInt(String.valueOf(number.charAt(i)));

            if (even) {
                digit *= 2;
                if (digit > 9) {
                    // "sum" the digits if we end up with a 2-digit number
                    digit = digit - 10 + 1;
                }
            }
            even = !even;

            sum += digit;
        }

        return sum % 10 == 0;
    }
}
