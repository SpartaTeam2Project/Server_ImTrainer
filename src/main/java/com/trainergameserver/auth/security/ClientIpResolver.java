package com.trainergameserver.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 요청한 클라이언트의 실제 IP. 로그인 시도 제한에 씁니다.
 * <ul>
 *   <li>프록시 없이 직접 받을 때 (client-ip.header 비움): 접속한 IP (request.getRemoteAddr())</li>
 *   <li>CloudFront, ALB, Nginx/Caddy 뒤에 둘 때 (client-ip.header=X-Forwarded-For):
 *       프록시가 덧붙인 X-Forwarded-For의 <b>마지막</b> 값.
 *       클라이언트가 X-Forwarded-For를 위조해 보내도 프록시는 실제 접속 IP를 맨 뒤에 붙이므로,
 *       마지막 값만 믿으면 위조할 수 없습니다. (프록시가 한 단계일 때)</li>
 * </ul>
 * 도메인을 붙여도 프록시 구성이 같으면 설정은 그대로입니다.
 */
@Component
public class ClientIpResolver {

    private final String header;

    public ClientIpResolver(@Value("${client-ip.header:}") String header) {
        this.header = header.isBlank() ? null : header.trim();
    }

    public String resolve(HttpServletRequest request) {
        if (header != null) {
            String value = request.getHeader(header);
            if (value != null) {
                String[] hops = value.split(",");
                String last = hops[hops.length - 1].trim();
                if (looksLikeIp(last)) {
                    return last;
                }
            }
        }
        return request.getRemoteAddr();
    }

    /** IPv4/IPv6 문자만 있는지 (호스트 이름이나 이상한 값이면 무시하고 접속 IP 사용) */
    private static boolean looksLikeIp(String value) {
        return !value.isEmpty() && value.length() <= 45 && value.matches("[0-9a-fA-F:.]+");
    }
}
