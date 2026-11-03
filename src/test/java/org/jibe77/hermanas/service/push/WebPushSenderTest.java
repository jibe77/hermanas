package org.jibe77.hermanas.service.push;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WebPushSenderTest {

    /** RFC 8291, appendix A: the full encryption example, byte for byte. */
    @Test
    void encryptMatchesRfc8291TestVector() throws Exception {
        byte[] plaintext = "When I grow up, I want to be a watermelon".getBytes(StandardCharsets.UTF_8);
        KeyPair asKeys = new KeyPair(
                WebPushSender.publicKey(WebPushSender.decode(
                        "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8")),
                WebPushSender.privateKey(WebPushSender.decode("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw")));
        byte[] uaPublic = WebPushSender.decode(
                "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] authSecret = WebPushSender.decode("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] salt = WebPushSender.decode("DGv6ra1nlYgDCS1FRnbzlw");

        byte[] body = WebPushSender.encrypt(plaintext, uaPublic, authSecret, asKeys, salt);

        assertEquals("DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN",
                Base64.getUrlEncoder().withoutPadding().encodeToString(body));
    }

    @Test
    void decodeAcceptsPaddedStandardBase64() {
        byte[] expected = {(byte) 0xfb, (byte) 0xff, 0x01};
        assertArrayEquals(expected, WebPushSender.decode("-_8B"));
        assertArrayEquals(expected, WebPushSender.decode("+/8B"));
        assertArrayEquals(new byte[]{1}, WebPushSender.decode("AQ=="));
    }
}
