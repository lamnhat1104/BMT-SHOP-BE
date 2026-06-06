package com.example.demo.account.service;

import com.example.demo.account.entity.SocialAccount;
import com.example.demo.account.entity.User;
import com.example.demo.account.repository.SocialAccountRepository;
import com.example.demo.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String clientRegistrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = clientRegistrationId.equals("google")
                ? (String) attributes.get("sub")
                : (String) attributes.get("id");

        String tempPicture = "";
        if (clientRegistrationId.equals("google")) {
            tempPicture = (String) attributes.get("picture");
        } else if (clientRegistrationId.equals("facebook")) {
            Map<String, Object> pictureObj = (Map<String, Object>) attributes.get("picture");
            if (pictureObj != null) {
                Map<String, Object> dataObj = (Map<String, Object>) pictureObj.get("data");
                tempPicture = (String) dataObj.get("url");
            }
        }

        final String picture = tempPicture;

        SocialAccount.Provider providerEnum = clientRegistrationId.equals("google")
                ? SocialAccount.Provider.google
                : SocialAccount.Provider.facebook;

        User user;
        String finalEmail;
        java.util.Optional<SocialAccount> existingSocial = socialAccountRepository.findByProviderAndProviderId(providerEnum, providerId);
        if (existingSocial.isPresent()) {
            user = existingSocial.get().getUser();
            finalEmail = user.getEmail();
            if (name != null && !name.trim().isEmpty() && !name.equals(user.getFullName())) {
                user.setFullName(name);
                userRepository.save(user);
            }
        } else {
            if (email == null || email.trim().isEmpty()) {
                email = providerId + "@" + clientRegistrationId + ".com";
            }
            finalEmail = email;
            user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(finalEmail);
                newUser.setFullName(name != null && !name.trim().isEmpty() ? name : (clientRegistrationId.equals("google") ? "Google User" : "Facebook User"));
                newUser.setAvatar(picture);
                newUser.setIsActive(true);
                return userRepository.save(newUser);
            });

            SocialAccount socialAccount = new SocialAccount();
            socialAccount.setUser(user);
            socialAccount.setProvider(providerEnum);
            socialAccount.setProviderId(providerId);
            socialAccount.setEmail(finalEmail);
            socialAccountRepository.save(socialAccount);
        }

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        if (userNameAttributeName == null || userNameAttributeName.trim().isEmpty()) {
            userNameAttributeName = clientRegistrationId.equals("google") ? "sub" : "id";
        }

        java.util.Map<String, Object> customAttributes = new java.util.HashMap<>(oAuth2User.getAttributes());
        customAttributes.put("email", finalEmail);
        customAttributes.put("provider", clientRegistrationId);
        customAttributes.put("providerId", providerId);

        return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                customAttributes,
                userNameAttributeName
        );
    }
}
