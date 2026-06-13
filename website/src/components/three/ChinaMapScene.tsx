'use client';

import { useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';

const CHINA_OUTLINE: [number, number][] = [
  [73.5, 39.5], [75, 37], [77, 35], [79, 33], [80, 31], [81, 30],
  [84, 28.5], [87, 28], [89, 27.5], [92, 27], [95, 28.5], [97, 28],
  [98.5, 25], [100, 22], [101, 21.5], [103, 22.5], [105, 23],
  [107, 21.5], [108, 21], [108.5, 18.5], [109.5, 18], [110, 20],
  [111, 21], [114, 22.5], [117, 23.5], [118.5, 25], [120, 26.5],
  [121.5, 28.5], [122, 30], [121.5, 31], [121, 32], [120, 33.5],
  [119, 35], [120.5, 36], [122, 37], [121.5, 38.5], [122.5, 40],
  [124, 40.5], [126, 41], [128, 42], [130, 43], [131, 45],
  [134, 47.5], [131, 47], [128, 49.5], [126, 49], [125, 52],
  [120, 52], [117, 48], [116, 47], [113, 44.5], [111, 44],
  [108, 42.5], [105, 42], [100, 42.5], [96, 43], [91, 45],
  [87, 49], [83, 47], [80, 44], [78, 41], [75, 40.5], [73.5, 39.5],
];

function Particles() {
  const pointsRef = useRef<THREE.Points>(null!);
  const timeRef = useRef(0);

  const { positions, colors } = useMemo(() => {
    const pos: number[] = [];
    const col: number[] = [];
    const jadeGreen = new THREE.Color('#0D7377');
    const gold = new THREE.Color('#C8963E');
    const pointCount = 2000;

    for (let i = 0; i < pointCount; i++) {
      const t = (i / pointCount) * (CHINA_OUTLINE.length - 1);
      const idx = Math.floor(t);
      const frac = t - idx;
      const nextIdx = Math.min(idx + 1, CHINA_OUTLINE.length - 1);

      const lng = CHINA_OUTLINE[idx][0] + frac * (CHINA_OUTLINE[nextIdx][0] - CHINA_OUTLINE[idx][0]);
      const lat = CHINA_OUTLINE[idx][1] + frac * (CHINA_OUTLINE[nextIdx][1] - CHINA_OUTLINE[idx][1]);

      const spread = Math.random() * 1.5;
      const angle = Math.random() * Math.PI * 2;
      const x = (lng - 105) * 0.08 + Math.cos(angle) * spread * 0.08;
      const y = (lat - 32) * 0.1 + Math.sin(angle) * spread * 0.1;
      const z = (Math.random() - 0.5) * 0.3;

      pos.push(x, y, z);

      const color = jadeGreen.clone().lerp(gold, Math.random());
      col.push(color.r, color.g, color.b);
    }

    return { positions: new Float32Array(pos), colors: new Float32Array(col) };
  }, []);

  useFrame((_, delta) => {
    timeRef.current += delta;
    if (pointsRef.current) {
      pointsRef.current.rotation.y = Math.sin(timeRef.current * 0.15) * 0.26;
    }
  });

  return (
    <points ref={pointsRef}>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
        <bufferAttribute attach="attributes-color" args={[colors, 3]} />
      </bufferGeometry>
      <pointsMaterial
        size={0.04}
        vertexColors
        transparent
        opacity={0.85}
        sizeAttenuation
        depthWrite={false}
      />
    </points>
  );
}

export default function ChinaMapScene() {
  return (
    <Canvas
      camera={{ position: [0, 0, 6], fov: 50 }}
      dpr={[1, 1.5]}
      style={{ background: 'transparent' }}
      gl={{ antialias: true, alpha: true }}
    >
      <Particles />
      <ambientLight intensity={0.5} />
    </Canvas>
  );
}
