#version 300 es
precision highp float;
// Halı yüzeyi: yün tüyleri, kir, köpük, ıslaklık, hasar ve gerçek zamanlı ışık.
in vec2 vUV;
uniform sampler2D uHaliTex;   // halı deseni (opak)
uniform sampler2D uEsyaTex;   // kirin altındaki hazineler (önçarpımlı alfa)
uniform sampler2D uKirTex;    // kir rengi + miktar (düz alfa)
uniform sampler2D uKopukTex;  // köpük
uniform sampler2D uIslakTex;  // ıslaklık (alfa)
uniform sampler2D uHasarTex;  // solma (alfa)
uniform vec2 uHaliPx;         // halının piksel boyutu
uniform vec2 uIzgara;         // kir ızgarası boyutu (hücre)
uniform vec3 uIsik;           // ışık yönü (x sağ, y aşağı, z izleyiciye)
uniform float uZaman;
uniform float uDalga;         // silkeleme dalgası genliği (0..1)
uniform float uYogunluk;      // ekran yoğunluğu (dp -> px)
out vec4 renk;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float gurultu(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float t = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        t += a * gurultu(p);
        p = p * 2.03 + 17.1;
        a *= 0.5;
    }
    return t;
}

// Yün tüyü yüksekliği: düğüm sıraları + ince lif gürültüsü
float tuy(vec2 px) {
    float s = 1.7 * uYogunluk;
    vec2 q = px / s;
    vec2 f = fract(q) - 0.5;
    float h = 0.6 + 0.4 * hash(floor(q));
    h *= 1.0 - dot(f, f) * 1.5;
    return h;
}

// Köpük kabarcıkları: hücresel gürültü
float kabarcik(vec2 px) {
    vec2 c = px / (6.0 * uYogunluk);
    vec2 ic = floor(c);
    vec2 fc = fract(c);
    float en = 1.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 g = vec2(float(x), float(y));
            vec2 o = vec2(hash(ic + g), hash(ic + g + 17.0));
            float r = 0.35 + 0.35 * hash(ic + g + 31.0);
            en = min(en, length(g + o - fc) / r);
        }
    }
    return en;
}

void main() {
    vec2 px = vUV * uHaliPx;
    float olcek = uYogunluk * 9.0;

    // Alan bükme: ızgara dokularının kenarları düzensiz ve doğal görünsün
    vec2 buk = (vec2(fbm(px / olcek), fbm(px / olcek + 9.3)) - 0.5) * 1.6 / uIzgara;
    vec2 uvB = vUV + buk;

    vec4 kir = texture(uKirTex, uvB);
    float kirDoku = fbm(px / (uYogunluk * 4.0));
    float ka = smoothstep(0.18, 0.5, kir.a - 0.28 * (kirDoku - 0.5) - 0.12 * hash(floor(px / (1.3 * uYogunluk))));
    ka *= smoothstep(0.0, 0.08, kir.a);
    // Lekelerin kenarı (kahve halkası gibi) biraz koyu
    float halka = smoothstep(0.25, 0.5, kir.a) * (1.0 - smoothstep(0.5, 0.85, kir.a));

    float kp = texture(uKopukTex, uvB).a;
    float kb = kp > 0.02 ? kabarcik(px) : 1.0;
    float kopuk = smoothstep(0.1, 0.45, kp + 0.15 * (kirDoku - 0.5));
    float kabarik = kopuk * (1.0 - smoothstep(0.0, 1.0, kb));

    float islak = smoothstep(0.0, 0.6, texture(uIslakTex, uvB).a);
    float hasar = texture(uHasarTex, uvB).a;

    // Yükseklik ve normal (ekran türevleriyle — tek değerlendirme)
    float h = tuy(px);
    float kirH = 0.5 + 0.9 * fbm(px / (uYogunluk * 2.5));
    h = mix(h, kirH, ka);
    h += kabarik * 1.6;
    vec3 N = normalize(vec3(-dFdx(h) * 1.4, -dFdy(h) * 1.4, 1.0));
    float faz = vUV.y * 10.0 - uZaman * 26.0;
    N = normalize(N + vec3(0.0, cos(faz) * uDalga * 0.9, 0.0));

    vec3 albedo = texture(uHaliTex, vUV).rgb;
    // Lif tonu
    albedo *= 0.92 + 0.16 * hash(floor(px * 0.9));
    vec4 esya = texture(uEsyaTex, vUV);
    albedo = albedo * (1.0 - esya.a) + esya.rgb;

    float lum = dot(albedo, vec3(0.299, 0.587, 0.114));
    albedo = mix(albedo, vec3(lum) * 0.8 + vec3(0.24, 0.21, 0.17), hasar * 0.85);

    vec3 kirRenk = kir.rgb * (0.62 + 0.75 * kirDoku) * (1.0 - 0.25 * halka);
    albedo = mix(albedo, kirRenk, ka);

    albedo *= 1.0 - 0.4 * islak;

    vec3 kopukRenk = mix(texture(uKopukTex, uvB).rgb, vec3(1.0), 0.4) * (0.88 + 0.12 * (1.0 - kb));
    albedo = mix(albedo, kopukRenk, kopuk * 0.95);

    // Işık
    vec3 L = normalize(uIsik);
    vec3 H = normalize(L + vec3(0.0, 0.0, 1.0));
    float dif = max(dot(N, L), 0.0);
    float parlaklik = mix(16.0, 110.0, max(islak, kopuk));
    float yansima = mix(0.05, 0.6, max(islak * 0.85, kopuk)) * (1.0 - ka * 0.75) + esya.a * 0.6;
    float spek = pow(max(dot(N, H), 0.0), parlaklik) * yansima;
    float kadife = pow(1.0 - max(N.z, 0.0), 2.0) * 0.4 * (1.0 - ka) * (1.0 - kopuk);
    vec3 c = albedo * (0.42 + 0.68 * dif) + vec3(spek) + albedo * kadife;

    // Kenar kalınlığı
    vec2 kenar = min(vUV, 1.0 - vUV) * uHaliPx;
    c *= 0.72 + 0.28 * smoothstep(0.0, 4.0 * uYogunluk, min(kenar.x, kenar.y));

    renk = vec4(c, 1.0);
}
