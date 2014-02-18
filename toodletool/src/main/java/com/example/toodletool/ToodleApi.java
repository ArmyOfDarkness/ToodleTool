package com.example.toodletool;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.exceptions.OAuthException;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.*;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;
import org.scribe.utils.Preconditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brendan on 1/26/14.
 */
public class ToodleApi extends DefaultApi20 {
    /*
    * string to authorize
    * change state to random string for each request in future!!!
     */
    private static final String AUTHORIZE_URL = "https://api.toodledo.com/3/account/authorize.php?response_type=code&client_id=ToodleTool&state=ToodleTest&scope=basic%20tasks%20notes%20write";
    private static final String ACCESS_URL = "https://%s:%s@api.toodledo.com/3/account/token.php";

    @Override
    public String getAccessTokenEndpoint() {

        return ACCESS_URL;
    }

    @Override
    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig oAuthConfig) {
        return AUTHORIZE_URL;
    }

    @Override
    public OAuthService createService(OAuthConfig config) {
        return new ToodleOAuth2Service(this, config);
    }

    @Override
    public AccessTokenExtractor getAccessTokenExtractor() {
        return new AccessTokenExtractor() {
            public Token extract(String response) {
                Preconditions.checkEmptyString(response, "Response body is incorrect. Can't extract a token from an empty string");
                Matcher matcher = Pattern.compile("\"access_token\":\"([^&\"]+)\"").matcher(response);
                if (matcher.find())
                {
                    String token = OAuthEncoder.decode(matcher.group(1));
                    return new Token(token, "", response);
                }
                else
                {
                    throw new OAuthException("Response body is incorrect. Can't extract a token from this: '" + response + "'", null);
                }

            }
        };
    }

    private class ToodleOAuth2Service extends OAuth20ServiceImpl {

        private static final String GRANT_TYPE = "grant_type";
        private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
        private static final String GRANT_TYPE_REFRESH_CODE = "refresh_token";
        private ToodleApi api;
        private OAuthConfig config;

        public ToodleOAuth2Service(ToodleApi api, OAuthConfig config) {
            super(api, config);
            this.api = api;
            this.config = config;
        }

        @Override
        public Token getAccessToken(Token requestToken, Verifier verifier) {
            OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), String.format(api.getAccessTokenEndpoint(), config.getApiKey(), config.getApiSecret()));
            switch (api.getAccessTokenVerb()) {
                case POST:
                    request.addBodyParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
                    if (config.getApiSecret() != null && config.getApiSecret().length() > 0)
                        request.addBodyParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
                    if (!RefreshTokenHolder.getInstance().getAccessStatus() && !RefreshTokenHolder.getInstance().getRefreshStatus()) {
                        request.addBodyParameter(OAuthConstants.CODE, verifier.getValue().split(";")[0]);
                    } else if (RefreshTokenHolder.getInstance().getRefreshStatus()) {
                        request.addBodyParameter(GRANT_TYPE_REFRESH_CODE, verifier.getValue());
                    }
                    request.addBodyParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
                    if (RefreshTokenHolder.getInstance().getRefreshStatus()) {
                        request.addBodyParameter(GRANT_TYPE, GRANT_TYPE_REFRESH_CODE);
                    } else {
                        request.addBodyParameter(GRANT_TYPE, GRANT_TYPE_AUTHORIZATION_CODE);
                    }
                    break;
                case GET:
                default:
                    request.addQuerystringParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
                    if (config.getApiSecret() != null && config.getApiSecret().length() > 0)
                        request.addQuerystringParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
                    request.addQuerystringParameter(OAuthConstants.CODE, verifier.getValue());
                    request.addQuerystringParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
                    if(config.hasScope()) request.addQuerystringParameter(OAuthConstants.SCOPE, config.getScope());
            }
            Response response = request.send();
            RefreshTokenHolder.getInstance().extractToken(response.getBody());
            return api.getAccessTokenExtractor().extract(response.getBody());
        }
    }
}
