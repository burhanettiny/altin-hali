#version 300 es
precision highp float;
// Cilalı parke zemin: dokudan kabartma, hareketli ışık parlaması.
in vec2 vUV;
uniform sampler2D uZeminTex;
uniform vec2 uEkran;
uniform vec3 uIsik;
out vec4 renk;
void main() {
    vec2 adim = 1.0 / uEkran;
    vec3 a = texture(uZeminTex, vUV).rgb;
    float l0 = dot(a, vec3(0.333));
    float lx = dot(texture(uZeminTex, vUV + vec2(adim.x, 0.0)).rgb, vec3(0.333));
    float ly = dot(texture(uZeminTex, vUV + vec2(0.0, adim.y)).rgb, vec3(0.333));
    vec3 N = normalize(vec3(-(lx - l0) * 3.0, -(ly - l0) * 3.0, 1.0));
    vec3 L = normalize(uIsik);
    vec3 H = normalize(L + vec3(0.0, 0.0, 1.0));
    float dif = max(dot(N, L), 0.0);
    // Cila: geniş, yumuşak bir yansıma lekesi (pencereden gelen ışık)
    vec2 isikNokta = vec2(0.5, 0.5) - uIsik.xy * 0.6;
    float pencere = exp(-length((vUV - isikNokta) * vec2(1.0, 0.6)) * 3.5) * 0.18;
    float spek = pow(max(dot(N, H), 0.0), 40.0) * 0.25;
    vec3 c = a * (0.55 + 0.55 * dif) + vec3(spek + pencere) * vec3(1.0, 0.95, 0.85);
    renk = vec4(c, 1.0);
}
