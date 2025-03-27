<#-- themes/your-theme-name/login/login.ftl -->
<#-- This template is responsible for rendering the main login page -->

<#import "template.ftl" as layout> <#-- Import the base layout template -->

<#-- Use the registrationLayout macro provided by template.ftl -->
<#-- Adjust displayInfo based on your theme's needs -->
<@layout.registrationLayout displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??) ; section>

<#-- Section: Header -->
    <#if section = "header">
        ${msg("doLogIn")} <#-- Display "Sign in" or similar localized text -->

    <#-- Section: Form -->
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#-- Check if password authentication is enabled for the realm -->
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">

                        <#-- Username / Email Input -->
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="username" class="${properties.kcLabelClass!}">
                                <#if !realm.loginWithEmailAllowed>${msg("username")}
                                <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                                <#else>${msg("email")}</#if>
                            </label>
                            <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}"
                                   type="text" autofocus autocomplete="off"
                                   aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                            />
                            <#-- Display username/password errors -->
                            <#if messagesPerField.existsError('username','password')>
                                <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                            </span>
                            </#if>
                        </div>

                        <#-- Password Input -->
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
                            <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="off"
                                   aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                            />
                            <#-- Display password errors (often same message as username error) -->
                            <#if usernameHidden?? && messagesPerField.existsError('password')>
                                <span id="input-error-password" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.getFirstError('password'))?no_esc}
                            </span>
                            </#if>
                        </div>

                        <#-- Remember Me & Forgot Password Row -->
                        <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                            <div id="kc-form-options">
                                <#-- Remember Me Checkbox (if enabled) -->
                                <#if realm.rememberMe && !usernameHidden??>
                                    <div class="checkbox">
                                        <label>
                                            <#if login.rememberMe??>
                                                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                                            <#else>
                                                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                                            </#if>
                                        </label>
                                    </div>
                                </#if>
                            </div>

                            <#-- Forgot Password Link (if password auth is enabled) -->
                            <div class="${properties.kcFormOptionsWrapperClass!}">
                                <#if realm.resetPasswordAllowed>
                                    <span><a tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                                </#if>
                            </div>
                        </div>

                        <#-- Login Button -->
                        <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                            <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                            <input tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                   name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                        </div>


                        <#-- *** START: Fingerprint Integration *** -->

                        <!-- Hidden input field that will be populated by JavaScript -->
                        <!-- The 'name' attribute ("browserFingerprint") is CRITICAL and must match Authenticator expectation -->
                        <input type="hidden" id="browserFingerprint" name="browserFingerprint" value=""/>

                        <!-- Include FingerprintJS library (use defer to avoid blocking) -->
                        <!-- Consider hosting this yourself or using subresource integrity (SRI) for security -->
                        <script src="https://cdn.jsdelivr.net/npm/@fingerprintjs/fingerprintjs@3/dist/fp.min.js" defer></script>

                        <!-- Script to generate and populate the fingerprint -->
                        <script>
                            // Wait for the DOM to be fully loaded before running the script
                            document.addEventListener('DOMContentLoaded', (event) => {
                                const FINGERPRINT_INPUT_ID = 'browserFingerprint'; // Match hidden input ID
                                const fpPromise = FingerprintJS.load(); // Load the library

                                (async () => {
                                    try {
                                        console.debug('FingerprintJS: Loading...'); // Optional: for debugging
                                        const fp = await fpPromise;
                                        console.debug('FingerprintJS: Getting visitorId...'); // Optional: for debugging
                                        const result = await fp.get(); // Get the fingerprint components and visitorId
                                        const visitorId = result.visitorId; // The stable fingerprint hash
                                        console.debug('FingerprintJS: VisitorId obtained:', visitorId); // Optional: for debugging

                                        // Find the hidden input field in the form
                                        const inputElement = document.getElementById(FINGERPRINT_INPUT_ID);

                                        if (inputElement) {
                                            // Set the value of the hidden input field
                                            inputElement.value = visitorId;
                                            console.debug(`FingerprintJS: Set hidden input #${FINGERPRINT_INPUT_ID} value.`); // Optional: for debugging
                                        } else {
                                            // Log an error if the hidden input isn't found (shouldn't happen if IDs match)
                                            console.error(`FingerprintJS: Could not find hidden input element with ID: ${FINGERPRINT_INPUT_ID}`);
                                        }
                                    } catch (error) {
                                        // Log any errors during fingerprint generation
                                        console.error('FingerprintJS: Error during initialization or fingerprint generation:', error);
                                        // IMPORTANT: Do not block login if fingerprinting fails. The backend should handle the missing value.
                                    }
                                })(); // Immediately invoke the async function
                            });
                        </script>
                        <#-- *** END: Fingerprint Integration *** -->

                    </form>
                </#if> <#-- End realm.password check -->
            </div> <#-- End kc-form-wrapper -->

            <#-- Social Login Providers (if configured) -->
            <#if realm.password && social.providers??>
                <div id="kc-social-providers" class="${properties.kcFormSocialAccountContentClass!} ${properties.kcFormSocialAccountClass!}">
                    <ul class="${properties.kcFormSocialAccountListClass!} <#if social.providers?size gt 4>${properties.kcFormSocialAccountListGridClass!}</#if>">
                        <#list social.providers as p>
                            <a id="social-${p.alias}" class="${properties.kcFormSocialAccountListLinkClass!} <#if social.providers?size gt 4>${properties.kcFormSocialAccountGridItem!}</#if>"
                               type="button" href="${p.loginUrl}">
                                <#if p.iconClasses?has_content>
                                    <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
                                    <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                                <#else>
                                    <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                                </#if>
                            </a>
                        </#list>
                    </ul>
                </div>
            </#if>

        </div> <#-- End kc-form -->

    <#-- Section: Info (Typically shows "Don't have an account? Register") -->
    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration-container">
                <div id="kc-registration">
                    <span>
                        ${msg("noAccount")}
                        <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a>
                    </span>
                </div>
            </div>
        </#if>
    </#if> <#-- End section checks -->

</@layout.registrationLayout> <#-- End layout macro -->