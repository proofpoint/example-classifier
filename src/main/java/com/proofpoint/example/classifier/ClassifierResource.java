package com.proofpoint.example.classifier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import javax.annotation.Nullable;
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
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/v1/classify")
public class ClassifierResource
{
    private final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11}");
    private final Pattern SSN_PATTERN = Pattern.compile("\\d{3}-\\d{2}-\\d{4}");
    private final StreamFlattener flattener = new StreamFlattener();
    private final Random random = new SecureRandom();

    private final ClassificationStore classificationStore;
    private final UsersOnHold usersOnHold;

    @Inject
    public ClassifierResource(UsersOnHold usersOnHold, ClassificationStore classificationStore)
    {
        this.classificationStore = classificationStore;
        this.usersOnHold = usersOnHold;
    }

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Number> classify(
            @Nullable @HeaderParam("X-NP-User-Name") String userId, @Nullable @HeaderParam("X-NP-HASH") String dataHash,
            final InputStream input)
            throws IOException
    {
        Map<String, Number> stored = null;
        if (dataHash != null) {
            stored = classificationStore.getClassifications(dataHash);
        }

        if (stored == null) {
            final Map<String, Number> contentClassifications = Maps.newHashMap();

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
                        contentClassifications.put("CreditCard", 100);
                    }

                    if (SSN_PATTERN.matcher(text).find()) {
                        contentClassifications.put("SSN", 100);
                    }

                    contentClassifications.put("Random", random.nextInt(100));
                }
            });

            if (dataHash != null) {
                classificationStore.setClassifications(dataHash, contentClassifications);
            }

            stored = contentClassifications;
        }

        Map<String, Number> classifications = Maps.newHashMap(stored);

        // check if user is on hold
        if (usersOnHold.contains(userId)) {
            classifications.put("Hold", 100);
        }

        return classifications;
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
