package no.nav.arbeidsgiver.min_side.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;

@Slf4j
@Service
public class GraphQlUtilsBatchSporring {
    @Value("classpath:pdl/hentPersonBolkRespons.person.graphql")
    Resource navnQueryResource;


    public String resourceAsString() throws IOException {
        String filinnhold = StreamUtils.copyToString(navnQueryResource.getInputStream(), Charsets.UTF_8);
        return filinnhold.replaceAll("\\s+", " ");
    }
}
