package org.zkoss.zkspringboot.security;

import java.net.URLEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * This is an example of minimal configuration for ZK + Spring Security, we open as less access as possible to run a ZK-based application.
 * Please understand the configuration and modify it upon your requirement.
 */
@Configuration
@EnableOAuth2Sso
@Order(value = 0)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private static final String ZUL_FILES = "/zkau/web/**/*.zul";
    private static final String ZK_RESOURCES = "/zkres/**";
    private static final String ZK_SECURE_RESOURCES = "/zkres/**/secure/**";
    // allow desktop cleanup after logout or when reloading login page
    private static final String REMOVE_DESKTOP_REGEX = "/zkau\\?dtid=.*&cmd_0=rmDesktop&.*";

    @Autowired
    private Environment env;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String logoutUrl = env.getProperty("endSessionEndpoint") + "?post_logout_redirect_uri=" +
                URLEncoder.encode(env.getProperty("homePage"), "UTF-8");
    	
    	// you need to disable spring CSRF to make ZK AU pass security filter
        // ZK already sends a AJAX request with a built-in CSRF token,
        // please refer to https://www.zkoss.org/wiki/ZK%20Developer's%20Reference/Security%20Tips/Cross-site%20Request%20Forgery
        http.csrf().disable();
        http.authorizeRequests()
            .antMatchers(ZUL_FILES, ZK_SECURE_RESOURCES).denyAll() // block direct access to zul under class path web resource folder
            .antMatchers(HttpMethod.GET, ZK_RESOURCES).permitAll() // allow zk resources
            .regexMatchers(HttpMethod.GET, REMOVE_DESKTOP_REGEX).permitAll() // allow desktop cleanup
            .requestMatchers(req -> "rmDesktop".equals(req.getParameter("cmd_0"))).permitAll() // allow desktop cleanup from ZATS
            .mvcMatchers("/","/login","/logout").permitAll() //permit the URL for login and logout
            .mvcMatchers("/secure").hasRole("USER")
            .anyRequest().authenticated() //enforce all requests to be authenticated
            .and()
//            .formLogin()
//            .loginPage("/login").defaultSuccessUrl("/secure/main")
//            .and()
//            .logout().logoutUrl("/logout").logoutSuccessUrl("/");
            .logout()
            .deleteCookies()
            .invalidateHttpSession(true)
            .logoutSuccessUrl(logoutUrl);
    }

    /**
     * Creates an {@link InMemoryUserDetailsManager} for demo/testing purposes only. DON'T use this in production, see: {@link User#withUserDetails}!
     * @return userDetailsService
     */
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        UserDetails user =
                User.withDefaultPasswordEncoder()
                        .username("user")
                        .password("password")
                        .roles("USER")
                        .build();

        return new InMemoryUserDetailsManager(user);
    }
}