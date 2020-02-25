package no.nav.tag.dittNavArbeidsgiver.utils;

import org.apache.commons.io.Charsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;

@Service
public class GraphQlUtils {
    @Value("classpath:pdl/hentPerson.navn.graphql")
    Resource navnQueryResource;

    public String resourceAsString() throws IOException {
        String filinnhold = StreamUtils.copyToString(navnQueryResource.getInputStream(), Charsets.UTF_8);
        return filinnhold.replaceAll("\\s+", " ");
    }
}
