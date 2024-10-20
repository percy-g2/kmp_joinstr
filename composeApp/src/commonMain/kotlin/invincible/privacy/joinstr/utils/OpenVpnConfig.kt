package invincible.privacy.joinstr.utils

object OpenVpnConfig {

    fun config(
        nodeUrl: String,
        serverCertificate: String,
        clientCertificate: String,
        key: String,
        vpnPort: String,
        vpnIpAddress: String,
        vpnHost: String
    ): String = "client\n" +
        "tls-client\n" +
        "dev tun\n" +
        "proto tcp\n" +
        "remote $vpnIpAddress $vpnPort\n" +
        "auth SHA512\n" +
        "cipher AES-256-GCM\n" +
        "keepalive 10 30\n" +
        "tls-cipher TLS-ECDHE-ECDSA-WITH-AES-256-GCM-SHA384\n" +
        "float\n" +
        "resolv-retry infinite\n" +
        "nobind\n" +
        "verb 3\n" +
        "persist-key\n" +
        "persist-tun\n" +
        "reneg-sec 0\n" +
        "pull\n" +
        "auth-nocache\n" +
        "script-security 2\n" +
        "tls-version-min 1.2\n" +
        "remote-cert-tls server\n" +
        "remote-cert-eku \"TLS Web Server Authentication\"\n" +
        "verify-x509-name $vpnHost name\n" +
        "route ${nodeUrl.removePrefix("http://")} 255.255.255.0 net_gateway\n" +
        "<ca>\n" +
        "$serverCertificate\n" +
        "</ca>\n" +
        "<cert>\n" +
        "$clientCertificate\n" +
        "</cert>\n" +
        "<key>\n" +
        "$key\n" +
        "</key>"
}