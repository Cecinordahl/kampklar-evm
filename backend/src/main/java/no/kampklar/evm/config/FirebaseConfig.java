package no.kampklar.evm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Initializes the Firebase Admin SDK from a service account JSON supplied via env var,
 * never a file committed to the repo. See FIREBASE_SERVICE_ACCOUNT_JSON in .env.example.
 */
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-json}")
    private String serviceAccountJson;

    @PostConstruct
    public void init() throws IOException {
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            // No secret configured (e.g. local `mvn test`) - skip init, Firestore/Auth beans are unused.
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);
    }

    @Bean
    @Lazy
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    @Bean
    @Lazy
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }
}
