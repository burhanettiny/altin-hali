#version 300 es
// Halı ızgarası: ekran pikseli koordinatlarında çizilir (Canvas katmanıyla birebir örtüşür).
in vec2 aUV;
uniform vec4 uHali;     // sol, üst, sağ, alt (piksel)
uniform vec2 uEkran;    // ekran boyutu (piksel)
uniform vec2 uKaydir;   // sarsıntı/silkeleme kayması (piksel)
out vec2 vUV;
void main() {
    vUV = aUV;
    vec2 p = mix(uHali.xy, uHali.zw, aUV) + uKaydir;
    gl_Position = vec4(p.x / uEkran.x * 2.0 - 1.0, 1.0 - p.y / uEkran.y * 2.0, 0.0, 1.0);
}
