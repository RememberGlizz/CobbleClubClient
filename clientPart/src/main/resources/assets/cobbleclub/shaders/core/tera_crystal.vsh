#version 150

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 vUv;
out vec3 vNormal;
out float vDepth;

void main() {
    vUv = UV0;
    vNormal = normalize(Normal);

    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    vDepth = -viewPos.z;

    gl_Position = ProjMat * viewPos;
}
