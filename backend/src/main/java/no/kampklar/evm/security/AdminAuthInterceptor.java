package no.kampklar.evm.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Lets a /admin/** request through only if it carries a valid, unrevoked Firebase ID token
 * belonging to the single provisioned admin UID: 401 for a missing or invalid token, 403 for
 * a valid token from anyone else. Fails closed - with no admin UID configured, nobody gets in.
 *
 * <p>An MVC interceptor rather than a servlet filter so Spring's CORS handling has already
 * run, and the browser sees the real 401/403 instead of an opaque CORS error.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthInterceptor.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectProvider<FirebaseAuth> firebaseAuth;
    private final String adminUid;

    // Resolved per request, not at startup: Firebase is not initialized when no service account
    // is configured (e.g. `mvn test`), and FirebaseAuth is a final class, so @Lazy cannot proxy it.
    public AdminAuthInterceptor(ObjectProvider<FirebaseAuth> firebaseAuth, @Value("${app.admin-uid}") String adminUid) {
        this.firebaseAuth = firebaseAuth;
        this.adminUid = adminUid;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true; // Browsers never send credentials on preflight; the real request is checked.
        }
        if (adminUid == null || adminUid.isBlank()) {
            log.warn("ADMIN_UID is not configured - rejecting {} {}", request.getMethod(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access is not configured");
            return false;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token");
            return false;
        }

        FirebaseToken token;
        try {
            // checkRevoked=true costs an extra lookup per call, which is fine for a single admin.
            token = firebaseAuth.getObject().verifyIdToken(header.substring(BEARER_PREFIX.length()), true);
        } catch (FirebaseAuthException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return false;
        }

        if (!adminUid.equals(token.getUid())) {
            log.warn("Non-admin uid {} attempted {} {}", token.getUid(), request.getMethod(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not an admin");
            return false;
        }
        return true;
    }
}
