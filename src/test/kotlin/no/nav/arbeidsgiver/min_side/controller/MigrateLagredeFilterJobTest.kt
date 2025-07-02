package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.lagredefilter.LagredeFilterService
import no.nav.arbeidsgiver.min_side.services.lagredefilter.MigrateLagredeFilterJob
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
    ]
)
class MigrateLagredeFilterJobTest {
    @Autowired
    lateinit var migrateLagredeFilterJob: MigrateLagredeFilterJob

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var lagredeFilterService: LagredeFilterService

    @Autowired
    lateinit var flyway: Flyway

    @BeforeEach
    fun setup() {
        flyway.clean()
        flyway.migrate()
    }

    @Test
    fun runMigration() {
        // insert into remoteStorage
        jdbcTemplate.update(
            """
            insert into storage (key, fnr, value, version, timestamp) values
            ('lagrede-filter', 
            '11111111111', 
            '[
  {
    "uuid": "6da7d33c-d9ec-4f38-98f0-8ab05a74d692",
    "navn": "dags filter",
    "filter": {
      "route": "/saksoversikt",
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [
        "810007702",
        "810008032",
        "810007982"
      ],
      "sortering": "FRIST",
      "sakstyper": [
        "Inntektsmelding"
      ],
      "oppgaveTilstand": []
    }
  },
  {
    "uuid": "f402c2fe-61f9-4829-b591-38c1ca619ab5",
    "navn": "dags filter2",
    "filter": {
      "route": "/saksoversikt",
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [
        "810007702",
        "810008032",
        "810007982"
      ],
      "sortering": "FRIST",
      "sakstyper": [],
      "oppgaveTilstand": []
    }
  },
  {
    "uuid": "54df4fe9-33cb-4296-9ed6-4c70e8ad95a1",
    "navn": "dag filter3",
    "filter": {
      "route": "/saksoversikt",
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [
        "810007702",
        "810007842",
        "810008032"
      ],
      "sortering": "FRIST",
      "sakstyper": [
        "Fritak arbeidsgiverperiode"
      ],
      "oppgaveTilstand": ["Ny"]
    }
  },
  {
    "uuid": "a4e6e91a-ad58-4f4f-abe6-046e22b87a64",
    "navn": "fager - med påminnelsefilter",
    "filter": {
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [],
      "sortering": "NYESTE",
      "sakstyper": [],
      "oppgaveFilter": [
        "TILSTAND_NY",
        "TILSTAND_NY_MED_PAAMINNELSE_UTLOEST"
      ]
    }
  },
  {
    "uuid": "147ec969-16a9-4dec-8360-6509d7cc653c",
    "navn": "fager - uløste oppgaver",
    "filter": {
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [],
      "sortering": "NYESTE",
      "sakstyper": [
        "fager"
      ],
      "oppgaveFilter": [
        "TILSTAND_NY"
      ]
    }
  }
]',
1,
'2025-04-04 09:57:03.017444+00')
        """.replace("\n", "").trimIndent()
        )

        jdbcTemplate.update(
            """
            insert into storage (key, fnr, value, version, timestamp) values
            ('lagrede-filter', 
            '22222222222', 
            '[
  {
    "uuid": "6da7333c-d9ec-4f38-98f0-8ab05a74d692",
    "navn": "dags filter",
    "filter": {
      "route": "/saksoversikt",
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [
        "810007702",
        "810008032",
        "810007982"
      ],
      "sortering": "FRIST",
      "sakstyper": [
        "Inntektsmelding"
      ],
      "oppgaveTilstand": []
    }
  },
  {
    "uuid": "f40232fe-61f9-4829-b591-38c1ca619ab5",
    "navn": "dags filter2",
    "filter": {
      "route": "/saksoversikt",
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [
        "810007702",
        "810008032",
        "810007982"
      ],
      "sortering": "FRIST",
      "sakstyper": [],
      "oppgaveTilstand": []
    }
  },
  {
    "uuid": "54df3fe9-33cb-4296-9ed6-4c70e8ad95a1",
    "navn": "dag filter3",
    "filter": {
      "route": "/saksoversikt",
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [
        "810007702",
        "810007842",
        "810008032"
      ],
      "sortering": "FRIST",
      "sakstyper": [
        "Fritak arbeidsgiverperiode"
      ],
      "oppgaveTilstand": ["Ny"]
    }
  },
  {
    "uuid": "a4e6391a-ad58-4f4f-abe6-046e22b87a64",
    "navn": "fager - med påminnelsefilter",
    "filter": {
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [],
      "sortering": "ELDSTE",
      "sakstyper": [],
      "oppgaveFilter": [
        "TILSTAND_NY",
        "TILSTAND_NY_MED_PAAMINNELSE_UTLOEST"
      ]
    }
  },
  {
    "uuid": "147e3969-16a9-4dec-8360-6509d7cc653c",
    "navn": "fager - uløste oppgaver",
    "filter": {
      "side": 1,
      "tekstsoek": "",
      "virksomheter": [],
      "sortering": "NYESTE",
      "sakstyper": [
        "fager"
      ],
      "oppgaveFilter": [
        "TILSTAND_NY"
      ]
    }
  }
]',
1,
'2025-04-04 09:57:03.017444+00')
        """.trimIndent()
        )

        migrateLagredeFilterJob.run()

        val lagredeFilter1 = lagredeFilterService.getAll("11111111111")
        val lagredeFilter2 = lagredeFilterService.getAll("22222222222")

        assertEquals(5, lagredeFilter1.count())
        assertEquals(5, lagredeFilter2.count())
    }
}