package com.proofpoint.classifier;

import com.google.common.collect.ImmutableMap;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.Map;
import java.util.Random;

@Path("/v1/classify")
public class ClassifierResource
{
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Number> post(InputStream input)
    {
        Random random = new Random();
        return ImmutableMap.<String, Number>builder()
                .put("classA", random.nextInt(100))
                .put("classB", random.nextInt(100))
                .put("classC", random.nextInt(100))
                .build();
    }
}
