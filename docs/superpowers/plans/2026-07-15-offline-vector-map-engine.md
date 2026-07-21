# Offline Vector Map Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the duplicated platform GeoJSON and hand-drawn national canvas with one offline, self-rendered vector map engine for China and its surrounding region, and make the restrained standard-map style the product default without regressing footprints, drill-down, themes, or gestures.

**Architecture:** A pure Kotlin Multiplatform `mapFormat` module owns the compiled package contract. A JVM `mapCompiler` consumes the existing administrative GeoJSON plus audited surrounding-region sources, preserves complete geometry, creates deterministic LOD/topology/index files, and writes one Compose resource package. Shared runtime code validates and lazily loads the package, builds a scene, indexes visible features, resolves labels, caches Compose paths, and renders all themes from the same geometry. The old renderer remains behind an explicit migration fallback until national, province, city, and district flows pass Android and iOS device gates.

**Tech Stack:** Kotlin 2.3.20, Kotlin Multiplatform, Compose Multiplatform 1.10.0 Canvas, kotlinx.serialization 1.8.1, Compose Resources, coroutines, Kotlin test, JVM command-line compiler, Android ADB/Perfetto/gfxinfo, Xcode/iOS Simulator/Instruments.

## Global Constraints

- The first screen is an actual usable map. Do not add a landing page, tutorial card, or map-style explanation to the home screen.
- The engine draws vectors at runtime. Do not use the supplied standard-map image, raster tiles, an online map SDK, or a network dependency.
- Geographic scope is China plus enough surrounding land and ocean for the approved national camera. Do not build an infinite world map.
- The standard theme is the new default. Unvisited provinces are near-white; visited provinces use one restrained low-saturation jade fill and a darker border.
- Existing water-ink, vintage, rice-paper, starry-night, and mountain-mist themes remain available and use the same authoritative geometry.
- Preserve all `Point`, `MultiPoint`, `LineString`, `MultiLineString`, `Polygon`, `MultiPolygon`, polygon holes, islands, and detached pieces. No largest-polygon or first-ring shortcuts are allowed.
- Keep the ten independent South China Sea segments already verified in `SouthChinaSea.DASH_SEGMENTS`, including the segment east of Taiwan.
- Runtime code must not simplify full datasets. LOD, quantization, topology, bounds, and grid indexes are build-time products.
- Cold start loads only national data. Province/city/district packs are lazy and use a bounded LRU.
- A corrupt manifest or hash fails closed to the legacy renderer during migration. A corrupt child pack keeps the last good parent scene visible.
- The compiled package target is at most 26 MiB. National-map incremental memory is at most 40 MiB; deepest single-city navigation is at most 75 MiB.
- Preserve the unrelated untracked `docs/superpowers/plans/2026-06-14-haptic-feedback.md`.
- After every UI/runtime behavior change, obey the project Iron Law: run `./gradlew installDebug`, launch and interact through ADB, capture before/after screenshots, inspect logs, and perform equivalent iPhone 17 Pro simulator verification.
- Do not describe development data as formally approved. Public release remains blocked until the modified map completes the required map review and the real review number is recorded.

---

### Task 1: Establish The Shared Package Contract

**Files:**
- Modify: `settings.gradle.kts`
- Create: `mapFormat/build.gradle.kts`
- Create: `mapFormat/src/commonMain/kotlin/com/mapchina/map/format/VectorPackage.kt`
- Create: `mapFormat/src/commonMain/kotlin/com/mapchina/map/format/PackedTopology.kt`
- Create: `mapFormat/src/commonTest/kotlin/com/mapchina/map/format/VectorPackageTest.kt`
- Modify: `shared/build.gradle.kts`

**Interfaces:**
- Produces `VectorPackageManifest`, `VectorResourceEntry`, `VectorLayerId`, `VectorSourceRecord`, and `MapBounds`.
- Produces complete packed geometry types: points, lines, polygons, multipolygons, rings, topology arcs, label anchors, and grid cells.
- Produces `VectorPackageManifest.validate()` with explicit validation errors.

- [ ] **Step 1: Add failing package-contract tests**

```kotlin
class VectorPackageTest {
    @Test
    fun polygon_keepsOuterRingAndHoles() {
        val polygon = PackedPolygon(
            outer = PackedRing(listOf(0, 1, 2, 3)),
            holes = listOf(PackedRing(listOf(4, 5, 6, 7)))
        )
        val geometry = PackedGeometry(polygons = listOf(polygon))

        assertEquals(4, geometry.polygons.single().outer.arcRefs.size)
        assertEquals(1, geometry.polygons.single().holes.size)
    }

    @Test
    fun manifest_rejectsMissingNationalResources() {
        val manifest = VectorPackageManifest(
            schemaVersion = 1,
            dataVersion = "2026.07.15-dev",
            compilerVersion = "1",
            reviewNumber = null,
            bounds = MapBounds(72.0, 3.0, 142.0, 55.0),
            sources = emptyList(),
            resources = emptyList()
        )

        assertEquals(
            setOf(
                VectorLayerId.NEIGHBOR_LAND,
                VectorLayerId.PROVINCES,
                VectorLayerId.NATIONAL_LABELS,
                VectorLayerId.TEN_DASH_LINE
            ),
            manifest.validate().missingRequiredLayers
        )
    }
}
```

- [ ] **Step 2: Run the contract test and verify red**

Run: `./gradlew :mapFormat:allTests --quiet`

Expected: FAIL because `mapFormat` and the package types do not exist.

- [ ] **Step 3: Add the KMP module and package types**

Add `include(":mapFormat")` and configure Android, JVM, iOS arm64, and iOS simulator arm64 targets. Add `implementation(project(":mapFormat"))` to `shared` commonMain.

Use one explicit, serialization-stable representation:

```kotlin
@Serializable
data class MapBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double
) {
    init {
        require(west < east)
        require(south < north)
        require(west >= -180.0 && east <= 180.0)
        require(south >= -90.0 && north <= 90.0)
    }
}

@Serializable
enum class VectorLayerId {
    NEIGHBOR_LAND,
    COASTLINE,
    GRATICULE,
    RIVERS_MAJOR,
    IMPORTANT_ISLANDS,
    NATIONAL_BOUNDARY,
    PROVINCES,
    TEN_DASH_LINE,
    NATIONAL_LABELS,
    CITIES,
    DISTRICTS,
    LOCAL_LABELS
}

@Serializable
data class PackedRing(val arcRefs: List<Int>)

@Serializable
data class PackedPolygon(
    val outer: PackedRing,
    val holes: List<PackedRing> = emptyList()
)

@Serializable
data class PackedGeometry(
    val points: List<Int> = emptyList(),
    val lines: List<List<Int>> = emptyList(),
    val polygons: List<PackedPolygon> = emptyList()
)

@Serializable
data class PackedFeature(
    val id: String,
    val name: String? = null,
    val adminCode: String? = null,
    val geometry: PackedGeometry,
    val bounds: MapBounds,
    val minZoom: Float,
    val maxZoom: Float,
    val labelAnchor: QuantizedPoint? = null
)
```

Arc references use TopoJSON semantics: a non-negative value follows an arc; a negative value resolves with `index = -ref - 1` and reverses it. Coordinates are flat delta-encoded integer pairs under one file-level transform.

- [ ] **Step 4: Run the new module and shared compile gates**

Run: `./gradlew :mapFormat:allTests :shared:compileKotlinAndroid :shared:compileKotlinIosSimulatorArm64 --quiet`

Expected: PASS.

- [ ] **Step 5: Commit the package contract**

```bash
git add settings.gradle.kts shared/build.gradle.kts mapFormat
git commit -m "feat(map): define shared vector package format"
```

---

### Task 2: Parse Every Supported GeoJSON Geometry

**Files:**
- Create: `mapCompiler/build.gradle.kts`
- Create: `mapCompiler/src/main/kotlin/com/mapchina/mapcompiler/Main.kt`
- Create: `mapCompiler/src/main/kotlin/com/mapchina/mapcompiler/GeoJsonGeometryParser.kt`
- Create: `mapCompiler/src/main/kotlin/com/mapchina/mapcompiler/SourceFeature.kt`
- Create: `mapCompiler/src/test/kotlin/com/mapchina/mapcompiler/GeoJsonGeometryParserTest.kt`
- Create: `mapCompiler/src/test/resources/full-geometry-feature-collection.json`
- Modify: `settings.gradle.kts`

**Interfaces:**
- Produces `GeoJsonGeometryParser.parse(raw, layer, minZoom, maxZoom): List<SourceFeature>`.
- Preserves stable IDs, names, admin codes, all polygon members, and every inner ring.
- Rejects non-finite or out-of-range coordinates with a source file and JSON path in the error.

- [ ] **Step 1: Write one fixture containing all geometry families**

The fixture must contain a `Point`, `MultiPoint`, `LineString`, `MultiLineString`, a `Polygon` with one hole, and a two-member `MultiPolygon` whose second member has one hole.

```kotlin
class GeoJsonGeometryParserTest {
    private val parser = GeoJsonGeometryParser()

    @Test
    fun parsesEveryGeometryWithoutDroppingMembersOrHoles() {
        val raw = javaClass.getResource("/full-geometry-feature-collection.json")!!.readText()
        val features = parser.parse(raw, VectorLayerId.PROVINCES, 0f, 8f)

        assertEquals(6, features.size)
        val polygon = features.single { it.id == "polygon-with-hole" }.polygons.single()
        assertEquals(1, polygon.holes.size)
        val multi = features.single { it.id == "multi-polygon" }
        assertEquals(2, multi.polygons.size)
        assertEquals(1, multi.polygons[1].holes.size)
    }

    @Test
    fun invalidCoordinateReportsFeatureAndPath() {
        val error = assertFailsWith<SourceGeometryException> {
            parser.parse(
                """{"type":"Feature","id":"bad","geometry":{"type":"Point","coordinates":[181,40]}}""",
                VectorLayerId.PROVINCES,
                0f,
                8f
            )
        }
        assertTrue(error.message!!.contains("bad"))
        assertTrue(error.message!!.contains("coordinates"))
    }
}
```

- [ ] **Step 2: Run the parser test and verify red**

Run: `./gradlew :mapCompiler:test --tests com.mapchina.mapcompiler.GeoJsonGeometryParserTest --quiet`

Expected: FAIL because the compiler module and parser do not exist.

- [ ] **Step 3: Implement structured parsing**

Use `kotlinx.serialization.json.JsonElement`; do not inspect geometry with regular expressions. Normalize a `Feature`, `FeatureCollection`, or raw geometry into `SourceFeature`:

```kotlin
data class SourcePolygon(
    val outer: List<GeoCoordinate>,
    val holes: List<List<GeoCoordinate>>
)

data class SourceFeature(
    val id: String,
    val name: String?,
    val adminCode: String?,
    val points: List<GeoCoordinate>,
    val lines: List<List<GeoCoordinate>>,
    val polygons: List<SourcePolygon>,
    val layer: VectorLayerId,
    val minZoom: Float,
    val maxZoom: Float
)
```

Close polygon rings exactly once, reject rings with fewer than four closed coordinates, retain source order, and derive IDs in this order: `feature.id`, `properties.adcode`, `properties.id`, explicit caller fallback. A file with no stable ID fails compilation.

- [ ] **Step 4: Run parser and module tests**

Run: `./gradlew :mapCompiler:test --quiet`

Expected: PASS.

- [ ] **Step 5: Commit full geometry parsing**

```bash
git add settings.gradle.kts mapCompiler
git commit -m "feat(map): parse complete GeoJSON geometry"
```

---

### Task 3: Compile Deterministic LOD Topology And Grid Indexes

**Files:**
- Create: `mapCompiler/src/main/kotlin/com/mapchina/mapcompiler/GeometrySimplifier.kt`
- Create: `mapCompiler/src/main/kotlin/com/mapchina/mapcompiler/TopologyCompiler.kt`
- Create: `mapCompiler/src/main/kotlin/com/mapchina/mapcompiler/SpatialGridCompiler.kt`
- Create: `mapCompiler/src/main/kotlin/com/mapchina/mapcompiler/PackageWriter.kt`
- Create: `mapCompiler/src/test/kotlin/com/mapchina/mapcompiler/TopologyCompilerTest.kt`
- Create: `mapCompiler/src/test/kotlin/com/mapchina/mapcompiler/PackageWriterTest.kt`

**Interfaces:**
- Produces quantized, delta-encoded, shared arcs at 1e-5 degree maximum round-trip error.
- Produces LOD0, LOD1, and LOD2 with topology-preserving simplification.
- Produces deterministic fixed-grid index cells and SHA-256 manifest entries.

- [ ] **Step 1: Write failing topology and determinism tests**

```kotlin
@Test
fun adjacentPolygonsShareOneBoundaryArc() {
    val output = compiler.compile(listOf(leftProvince, rightProvince), tolerance = 0.01)
    val leftRefs = output.features.single { it.id == "left" }.geometry.polygons.single().outer.arcRefs
    val rightRefs = output.features.single { it.id == "right" }.geometry.polygons.single().outer.arcRefs

    assertTrue(leftRefs.any { leftRef -> rightRefs.contains(-leftRef - 1) })
}

@Test
fun simplificationNeverDeletesHoleOrDetachedPolygon() {
    val output = compiler.compile(listOf(multiPolygonWithHole), tolerance = 0.05)
    val geometry = output.features.single().geometry

    assertEquals(2, geometry.polygons.size)
    assertEquals(1, geometry.polygons.first().holes.size)
}

@Test
fun sameInputProducesByteIdenticalPackage() {
    val first = writer.writeToBytes(listOf(sourceB, sourceA), fixedBuildEpochMillis = 0L)
    val second = writer.writeToBytes(listOf(sourceA, sourceB), fixedBuildEpochMillis = 0L)

    assertContentEquals(first, second)
}
```

- [ ] **Step 2: Run compiler tests and verify red**

Run: `./gradlew :mapCompiler:test --tests '*TopologyCompilerTest' --tests '*PackageWriterTest' --quiet`

Expected: FAIL because topology, LOD, index, and writer classes do not exist.

- [ ] **Step 3: Implement quantization, arc reuse, LOD, and indexing**

Use one quantization transform per resource file. Split polygon edges at every shared vertex before hashing arcs. Canonicalize an arc by choosing the lexicographically smaller of forward and reversed coordinate sequences; negative references preserve reversed use.

Use these build-time tolerances unless an audit proves a smaller value is required:

```kotlin
data class LodPolicy(val id: Int, val toleranceDegrees: Double, val minZoom: Float)

val nationalLods = listOf(
    LodPolicy(id = 0, toleranceDegrees = 0.035, minZoom = 0f),
    LodPolicy(id = 1, toleranceDegrees = 0.010, minZoom = 5f),
    LodPolicy(id = 2, toleranceDegrees = 0.0025, minZoom = 7.5f)
)
```

Douglas-Peucker may simplify open arc interiors, but it must retain arc endpoints, keep every valid ring above four closed points, keep winding, and back off the tolerance if a ring self-intersects or a hole anchor leaves its outer polygon.

Create grid cells in geographic coordinates with deterministic cell IDs:

```kotlin
fun cellId(lng: Double, lat: Double, cellDegrees: Double): Long {
    val x = kotlin.math.floor((lng + 180.0) / cellDegrees).toLong()
    val y = kotlin.math.floor((lat + 90.0) / cellDegrees).toLong()
    return (y shl 32) or (x and 0xffffffffL)
}
```

Use 5-degree national cells, 1-degree city cells, and 0.25-degree district cells. Sort resources by path, features by stable ID, cells by ID, and feature IDs within cells before compact JSON serialization.

- [ ] **Step 4: Run all compiler tests twice and compare output**

Run: `./gradlew :mapCompiler:cleanTest :mapCompiler:test --quiet && ./gradlew :mapCompiler:cleanTest :mapCompiler:test --quiet`

Expected: both runs PASS with no golden-file diff.

- [ ] **Step 5: Commit the deterministic compiler core**

```bash
git add mapCompiler/src/main mapCompiler/src/test
git commit -m "feat(map): compile deterministic vector topology"
```

---

### Task 4: Build And Audit The First Offline Data Package

**Files:**
- Modify: `.gitignore`
- Create: `map-data/README.md`
- Create: `map-data/sources.json`
- Create: `map-data/ten-dash-line.geojson`
- Create: `mapCompiler/src/main/kotlin/com/mapchina/mapcompiler/AdministrativeSourceCatalog.kt`
- Create: `mapCompiler/src/main/kotlin/com/mapchina/mapcompiler/MapDataAudit.kt`
- Create: `mapCompiler/src/test/kotlin/com/mapchina/mapcompiler/MapDataAuditTest.kt`
- Create: `mapCompiler/src/test/kotlin/com/mapchina/mapcompiler/LegacyAssetImportTest.kt`
- Create: `shared/src/commonMain/composeResources/files/map-package/manifest.json`
- Create: `shared/src/commonMain/composeResources/files/map-package/base/*`
- Create: `shared/src/commonMain/composeResources/files/map-package/china/*`
- Create: `shared/src/commonMain/composeResources/files/map-package/cities/*`
- Create: `shared/src/commonMain/composeResources/files/map-package/districts/*`
- Create: `shared/src/commonMain/composeResources/files/map-package/labels/*`

**Data policy:**
- Existing administrative GeoJSON is the initial development geometry and remains marked as such in provenance.
- Surrounding land/coast and major rivers come from the Natural Earth maintained vector repository and are clipped to the package bounds.
- `GS(2023)2767` is recorded as a visual/compliance reference, not as the app review number.
- China geometry from the surrounding-land source is excluded so it cannot overwrite the app's audited administrative geometry.
- `reviewNumber` stays `null` in development packages.

- [ ] **Step 1: Add failing data-audit tests**

```kotlin
class MapDataAuditTest {
    @Test
    fun nationalPackageContainsRequiredAdministrativeCoverage() {
        val report = audit(packageDirectory)
        assertEquals(34, report.provinceCount)
        assertTrue(report.hasAdminCode("710000"))
        assertTrue(report.hasAdminCode("810000"))
        assertTrue(report.hasAdminCode("820000"))
        assertEquals(10, report.tenDashSegmentCount)
        assertTrue(report.hasDashEastOfTaiwan)
        assertTrue(report.packageBounds.contains(73.5, 3.0))
        assertTrue(report.packageBounds.contains(135.1, 53.6))
    }

    @Test
    fun packageFitsInstalledPayloadBudget() {
        assertTrue(audit(packageDirectory).totalBytes <= 26L * 1024L * 1024L)
    }
}
```

- [ ] **Step 2: Fetch reproducible surrounding-region sources**

Run:

```bash
mkdir -p map-data/downloads
curl -fL https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_admin_0_countries.geojson -o map-data/downloads/ne_10m_admin_0_countries.geojson
curl -fL https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_rivers_lake_centerlines.geojson -o map-data/downloads/ne_10m_rivers_lake_centerlines.geojson
```

Record the downloaded byte length and SHA-256 in `map-data/sources.json`. Keep source URLs, license, fetched commit/version, geographic role, and the `GS(2023)2767` reference record in the same file. Add `map-data/downloads/` to `.gitignore`; commit provenance and generated package output, not the cache files.

- [ ] **Step 3: Generate ten-dash source from the verified model**

Write ten GeoJSON `LineString` features from `SouthChinaSea.DASH_SEGMENTS` in source order. Add a parity test that compares every source coordinate to the Kotlin constant with a maximum absolute difference of `1e-6` degrees.

- [ ] **Step 4: Implement legacy administrative import and package assembly**

Derive hierarchy from six-digit adcodes and feature properties:

```kotlin
fun administrativeLevel(adcode: String): AdministrativeLevel = when {
    adcode.endsWith("0000") -> AdministrativeLevel.PROVINCE
    adcode.endsWith("00") -> AdministrativeLevel.CITY
    else -> AdministrativeLevel.DISTRICT
}
```

Special municipalities retain their district files under their province pack. Compile:

- National startup: neighbor land, coastline, province LOD0, national/province labels, ten-dash line, graticule metadata.
- National refinement: province LOD1/LOD2, major rivers, important islands.
- Province lazy packs: city geometry and local labels.
- City lazy packs: district geometry and local labels.

Run:

```bash
./gradlew :mapCompiler:run --args='compile --boundaries androidApp/src/main/assets/boundaries --districts androidApp/src/main/assets/districts --neighbors map-data/downloads/ne_10m_admin_0_countries.geojson --rivers map-data/downloads/ne_10m_rivers_lake_centerlines.geojson --ten-dash map-data/ten-dash-line.geojson --output shared/src/commonMain/composeResources/files/map-package'
```

Expected: the command exits 0, writes a deterministic manifest, and prints province/city/district counts, source hashes, layer sizes, and total bytes.

- [ ] **Step 5: Run audits and inspect rendered compiler previews**

Run:

```bash
./gradlew :mapCompiler:test --tests '*MapDataAuditTest' --tests '*LegacyAssetImportTest' --quiet
./gradlew :mapCompiler:run --args='preview --package shared/src/commonMain/composeResources/files/map-package --output build/map-previews'
```

Expected: tests PASS. Previews include `national-lod0.png`, `national-lod1.png`, `taiwan-and-east-dash.png`, `hainan-and-south-sea.png`, and one hole/multipolygon diagnostic. Compare these against the approved standard-map reference before committing.

- [ ] **Step 6: Commit package provenance and generated data**

```bash
git add .gitignore map-data/README.md map-data/sources.json map-data/ten-dash-line.geojson mapCompiler shared/src/commonMain/composeResources/files/map-package
git commit -m "feat(map): bundle audited offline vector package"
```

---

### Task 5: Validate And Load Shared Compose Resources

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/MapResourceReader.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/ComposeMapResourceReader.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorMapPackageLoader.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/PackageHash.kt`
- Create: `shared/src/androidMain/kotlin/com/mapchina/map/vector/PackageHash.android.kt`
- Create: `shared/src/iosMain/kotlin/com/mapchina/map/vector/PackageHash.ios.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorMapPackageLoaderTest.kt`

**Interfaces:**
- `MapResourceReader.read(path): ByteArray`
- `VectorMapPackageLoader.loadManifest(): VectorPackageManifest`
- `VectorMapPackageLoader.load(resource): PackedTopology`
- `PackageLoadException(reason, path)` with missing, malformed, schema, and hash failure categories.

- [ ] **Step 1: Add failing loader and integrity tests**

```kotlin
@Test
fun validResourceLoadsAfterHashVerification() = runTest {
    val reader = FakeMapResourceReader(validFiles)
    val loader = VectorMapPackageLoader(reader, PackageHash::sha256)

    val manifest = loader.loadManifest()
    val topology = loader.load(manifest.requireResource("china/provinces-lod0.topo"))

    assertEquals(34, topology.features.size)
}

@Test
fun mismatchedHashIsRejectedBeforeDecode() = runTest {
    val reader = FakeMapResourceReader(validFiles + ("china/provinces-lod0.topo" to byteArrayOf(1)))
    val loader = VectorMapPackageLoader(reader, PackageHash::sha256)

    val error = assertFailsWith<PackageLoadException> {
        loader.load(loader.loadManifest().requireResource("china/provinces-lod0.topo"))
    }
    assertEquals(PackageFailure.HASH_MISMATCH, error.reason)
}
```

- [ ] **Step 2: Run the loader test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.vector.VectorMapPackageLoaderTest --quiet`

Expected: FAIL because no shared loader exists.

- [ ] **Step 3: Implement resource reading and platform hashes**

```kotlin
class ComposeMapResourceReader : MapResourceReader {
    override suspend fun read(path: String): ByteArray =
        Res.readBytes("files/map-package/$path")
}

class VectorMapPackageLoader(
    private val reader: MapResourceReader,
    private val sha256: (ByteArray) -> String
) {
    private val json = Json { ignoreUnknownKeys = false }

    suspend fun load(entry: VectorResourceEntry): PackedTopology {
        val bytes = reader.read(entry.path)
        if (sha256(bytes) != entry.sha256) {
            throw PackageLoadException(PackageFailure.HASH_MISMATCH, entry.path)
        }
        return json.decodeFromString(bytes.decodeToString())
    }
}
```

Android uses `java.security.MessageDigest`. iOS uses `platform.CommonCrypto.CC_SHA256` with pinned input/output buffers. Add known-vector tests for empty bytes and `"abc"`, and compile the iOS target so platform hash code is checked.

- [ ] **Step 4: Run Android unit and iOS compile gates**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.vector.*' :shared:compileKotlinIosSimulatorArm64 --quiet`

Expected: PASS.

- [ ] **Step 5: Commit the loader**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/vector shared/src/androidMain/kotlin/com/mapchina/map/vector shared/src/iosMain/kotlin/com/mapchina/map/vector shared/src/commonTest/kotlin/com/mapchina/map/vector
git commit -m "feat(map): load verified shared vector resources"
```

---

### Task 6: Decode Geometry And Replace Linear Hit Testing

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorGeometry.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorGeometryDecoder.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorSpatialIndex.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorHitTester.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorGeometryDecoderTest.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorSpatialIndexTest.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorHitTesterTest.kt`

**Interfaces:**
- Decodes forward and reversed delta arcs to immutable geographic geometry.
- Queries candidate features by viewport or point without scanning every feature.
- Treats holes as excluded area and any member of a multipolygon as a valid hit.

- [ ] **Step 1: Write failing geometry and hit tests**

```kotlin
@Test
fun reversedArcDecodesWithoutDuplicatingJoinVertex() {
    val geometry = decoder.decode(twoArcPolygonWithSecondArcReversed)
    assertEquals(
        listOf(
            GeoPoint(0.0, 0.0),
            GeoPoint(2.0, 0.0),
            GeoPoint(2.0, 2.0),
            GeoPoint(0.0, 2.0),
            GeoPoint(0.0, 0.0)
        ),
        geometry.polygons.single().outer.points
    )
}

@Test
fun pointInsideHoleDoesNotHitFeature() {
    val tester = VectorHitTester(indexOf(squareWithSquareHole))
    assertNull(tester.hit(GeoPoint(5.0, 5.0)))
    assertEquals("region", tester.hit(GeoPoint(2.0, 2.0))?.id)
}

@Test
fun detachedIslandHitsSameAdministrativeFeature() {
    val tester = VectorHitTester(indexOf(mainlandAndIsland))
    assertEquals("region", tester.hit(islandCenter)?.id)
}
```

- [ ] **Step 2: Run focused tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.vector.Vector*Test' --quiet`

Expected: FAIL because decoder, index, and hit tester do not exist.

- [ ] **Step 3: Implement immutable runtime geometry and indexed queries**

```kotlin
data class VectorRing(val points: List<GeoPoint>)

data class VectorPolygon(
    val outer: VectorRing,
    val holes: List<VectorRing>
)

data class VectorFeature(
    val id: String,
    val name: String?,
    val adminCode: String?,
    val points: List<GeoPoint>,
    val lines: List<List<GeoPoint>>,
    val polygons: List<VectorPolygon>,
    val bounds: GeoBounds,
    val minZoom: Float,
    val maxZoom: Float,
    val labelAnchor: GeoPoint?
)

fun contains(point: GeoPoint, polygon: VectorPolygon): Boolean =
    pointInRing(point, polygon.outer) && polygon.holes.none { pointInRing(point, it) }
```

Use the compiled grid to narrow candidates, de-duplicate IDs when a feature spans cells, verify bounds, then run exact geometry. Preserve manifest feature order only for rendering; use deterministic topmost layer/order for hit conflict resolution.

- [ ] **Step 4: Run vector and legacy map tests**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.vector.*' --tests 'com.mapchina.map.*' --quiet`

Expected: PASS.

- [ ] **Step 5: Commit vector decoding and hit testing**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/vector shared/src/commonTest/kotlin/com/mapchina/map/vector
git commit -m "feat(map): decode and index complete geometry"
```

---

### Task 7: Add Lazy Scene Loading And Bounded Caching

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorMapScene.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorMapSceneRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorPackageCache.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorMapSceneRepositoryTest.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorPackageCacheTest.kt`

**Interfaces:**
- `scene: StateFlow<VectorMapSceneState>` exposes `Loading`, `Ready`, and `RecoverableFailure`.
- `loadNational()`, `loadProvince(adcode)`, `loadCity(adcode)`, and `returnTo(parent)` swap scenes atomically.
- LRU limits both entry count and estimated decoded bytes.

- [ ] **Step 1: Add failing loading, atomic-swap, and LRU tests**

```kotlin
@Test
fun childPackKeepsParentVisibleUntilComplete() = runTest {
    repository.loadNational()
    val parent = assertIs<VectorMapSceneState.Ready>(repository.scene.value).scene

    val job = launch { repository.loadProvince("330000") }
    advanceUntilIdleExceptDeferredRead()
    assertEquals(parent, assertIs<VectorMapSceneState.Loading>(repository.scene.value).previous)

    deferredRead.complete(zhejiangCityPack)
    job.join()
    assertEquals("330000", assertIs<VectorMapSceneState.Ready>(repository.scene.value).scene.scopeId)
}

@Test
fun corruptChildPackReturnsRecoverableFailureWithParentScene() = runTest {
    repository.loadNational()
    loader.failNext(PackageFailure.HASH_MISMATCH)
    repository.loadProvince("330000")

    val state = assertIs<VectorMapSceneState.RecoverableFailure>(repository.scene.value)
    assertEquals(VectorLayerScope.NATIONAL, state.previous.scope)
}
```

- [ ] **Step 2: Run repository tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.vector.VectorMapSceneRepositoryTest' --tests 'com.mapchina.map.vector.VectorPackageCacheTest' --quiet`

Expected: FAIL because scene loading and cache do not exist.

- [ ] **Step 3: Implement structured scene ownership**

```kotlin
data class VectorMapScene(
    val dataVersion: String,
    val scope: VectorLayerScope,
    val scopeId: String?,
    val layers: Map<VectorLayerId, VectorLayer>,
    val indexes: Map<VectorLayerId, VectorSpatialIndex>,
    val labels: List<VectorLabel>
)

sealed interface VectorMapSceneState {
    data object Idle : VectorMapSceneState
    data class Loading(val requestedScopeId: String?, val previous: VectorMapScene?) : VectorMapSceneState
    data class Ready(val scene: VectorMapScene) : VectorMapSceneState
    data class RecoverableFailure(
        val requestedScopeId: String?,
        val failure: PackageLoadException,
        val previous: VectorMapScene
    ) : VectorMapSceneState
    data class FatalFailure(val failure: PackageLoadException) : VectorMapSceneState
}
```

Cache decoded packages by `dataVersion/path`. Default limits: 6 child packs and 36 MiB decoded estimate. National startup packages are pinned. Cancel superseded child requests and ignore stale completions using an incrementing request generation.

- [ ] **Step 4: Run scene tests and shared test suite**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.vector.*' --quiet && ./gradlew :shared:allTests --quiet`

Expected: PASS.

- [ ] **Step 5: Commit the scene repository**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/vector shared/src/commonTest/kotlin/com/mapchina/map/vector
git commit -m "feat(map): load vector scenes on demand"
```

---

### Task 8: Define The Standard Theme And Layer Style Sheet

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapTheme.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/MapStyleSheet.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/map/MapThemeTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/map/MapThemeVisualStyleTest.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/MapStyleSheetTest.kt`

**Interfaces:**
- Adds `MapTheme.STANDARD(displayName = "标准")` as the fallback for null/unknown settings.
- Retains `MapTheme.DEFAULT(displayName = "经典")` so existing saved values do not silently change identity.
- Maps each theme to one geometry-independent `MapStyleSheet`.

- [ ] **Step 1: Add failing default and style tests**

```kotlin
@Test
fun missingPreferenceUsesStandardTheme() {
    assertEquals(MapTheme.STANDARD, MapTheme.fromName(null))
    assertEquals(MapTheme.STANDARD, MapTheme.fromName("removed-theme"))
    assertEquals(MapTheme.DEFAULT, MapTheme.fromName("DEFAULT"))
}

@Test
fun standardThemeUsesRestrainedFootprintContrast() {
    val style = MapStyleSheet.forTheme(MapTheme.STANDARD)
    assertEquals(Color(0xFFFBFCFA), style.unvisitedProvinceFill)
    assertEquals(Color(0xFFA9CBC0), style.visitedProvinceFill)
    assertEquals(Color(0xFF5F8F83), style.visitedProvinceStroke)
    assertTrue(style.graticule.alpha < style.provinceBoundary.alpha)
    assertTrue(style.neighborLandFill.luminance() < style.unvisitedProvinceFill.luminance())
}
```

- [ ] **Step 2: Run theme tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.MapTheme*' --tests 'com.mapchina.map.vector.MapStyleSheetTest' --quiet`

Expected: FAIL because `STANDARD` and layer styles do not exist.

- [ ] **Step 3: Implement explicit visual tokens**

```kotlin
data class StrokeStyle(val color: Color, val widthDp: Float, val alpha: Float = 1f)

data class MapStyleSheet(
    val oceanFill: Color,
    val neighborLandFill: Color,
    val chinaLandFill: Color,
    val unvisitedProvinceFill: Color,
    val visitedProvinceFill: Color,
    val selectedProvinceFill: Color,
    val neighborBoundary: StrokeStyle,
    val nationalHalo: StrokeStyle,
    val nationalBoundary: StrokeStyle,
    val provinceBoundary: StrokeStyle,
    val river: StrokeStyle,
    val graticule: StrokeStyle,
    val tenDashLine: StrokeStyle,
    val primaryLabel: Color,
    val secondaryLabel: Color
)
```

The standard theme uses no texture resource. Existing themes may keep texture/background resources, but all boundary widths and hierarchy come from the style sheet instead of hard-coded renderer colors.

- [ ] **Step 4: Run all theme tests**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.MapTheme*' --tests 'com.mapchina.map.vector.MapStyleSheetTest' --quiet`

Expected: PASS.

- [ ] **Step 5: Commit the default theme contract**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/MapTheme.kt shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt shared/src/commonMain/kotlin/com/mapchina/map/vector/MapStyleSheet.kt shared/src/commonTest/kotlin/com/mapchina/map
git commit -m "feat(map): add restrained standard map theme"
```

---

### Task 9: Build Path, Graticule, And Stable Label Engines

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorPathCache.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/GraticuleBuilder.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorLabelEngine.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorPathCacheTest.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/GraticuleBuilderTest.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorLabelEngineTest.kt`

**Interfaces:**
- Builds Compose paths with `PathFillType.EvenOdd` so holes remain transparent.
- Caches by data version, resource path, feature ID, LOD, and quantized projection state.
- Generates sparse clipped graticules and collision-free labels with stable ordering and zoom hysteresis.

- [ ] **Step 1: Add failing path and label tests**

```kotlin
@Test
fun polygonPathUsesEvenOddFillForHoles() {
    val path = cache.pathFor(featureWithHole, projection, lod = 0)
    assertEquals(PathFillType.EvenOdd, path.fillType)
}

@Test
fun smallCameraMoveReusesQuantizedPath() {
    cache.pathFor(feature, projectionAt(104.0000, 35.0000, 3.5f), 0)
    cache.pathFor(feature, projectionAt(104.0001, 35.0001, 3.5f), 0)
    assertEquals(1, cache.stats.buildCount)
}

@Test
fun labelsUsePriorityThenStableIdAndDoNotOverlap() {
    val placements = engine.place(overlappingLabels, viewport, zoom = 4f, previous = emptyMap())
    assertEquals(listOf("selected", "visited", "province"), placements.map { it.id })
    assertTrue(placements.pairs().none { (a, b) -> a.bounds.overlaps(b.bounds) })
}
```

- [ ] **Step 2: Run focused tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests '*VectorPathCacheTest' --tests '*GraticuleBuilderTest' --tests '*VectorLabelEngineTest' --quiet`

Expected: FAIL because rendering utilities do not exist.

- [ ] **Step 3: Implement bounded caches and mobile-density rules**

National graticules use 10-degree meridians and 5-degree parallels clipped to the approved camera bounds. Do not draw coordinate numbers on the mobile national view.

Use label priority in this order: selected region, footprint/photo-linked label, current scope, province, provincial capital, ordinary city, river/surrounding geography. Sort equal priority by stable ID. Keep a shown label until it falls 0.25 zoom below its threshold; keep a hidden label out until it rises 0.15 above the threshold. Reject placements outside a 6 dp viewport inset.

Path cache limits: 1,200 entries or 24 MiB estimated path data, whichever comes first. Clear only entries whose data version changes; theme changes reuse paths.

- [ ] **Step 4: Run utility and map tests**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.vector.*' --tests 'com.mapchina.map.*' --quiet`

Expected: PASS.

- [ ] **Step 5: Commit rendering utilities**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/vector shared/src/commonTest/kotlin/com/mapchina/map/vector
git commit -m "feat(map): cache paths and place map labels"
```

---

### Task 10: Render The National Vector Scene Behind A Migration Flag

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorMapRenderer.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorRenderPlan.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorRenderPlanTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/map/MapThemeVisualStyleTest.kt`

**Interfaces:**
- `VectorRenderPlan.build(scene, viewport, style, businessState)` produces deterministic visible draw commands.
- `VectorMapRenderer.draw(scope, plan)` draws the approved layer order.
- `RenderState.vectorScene` and `RenderState.vectorEngineEnabled` allow atomic migration with legacy fallback.

- [ ] **Step 1: Capture the required pre-change device evidence**

Run:

```bash
./gradlew installDebug
adb shell monkey -p com.mapchina.app -c android.intent.category.LAUNCHER 1
sleep 3
mkdir -p .superpowers/sdd/offline-vector-map
adb exec-out screencap -p > .superpowers/sdd/offline-vector-map/android-before.png
adb logcat -c
```

Build and launch the current iOS app on iPhone 17 Pro simulator and save `.superpowers/sdd/offline-vector-map/ios-before.png`. Record the exact simulator UDID and build command in `.superpowers/sdd/offline-vector-map/verification.md`.

- [ ] **Step 2: Add failing render-plan tests**

```kotlin
@Test
fun nationalPlanUsesApprovedLayerOrder() {
    val plan = VectorRenderPlan.build(nationalScene, viewport, standardStyle, businessState)
    assertEquals(
        listOf(
            DrawLayer.OCEAN,
            DrawLayer.GRATICULE,
            DrawLayer.NEIGHBOR_LAND,
            DrawLayer.CHINA_LAND,
            DrawLayer.REGION_FILL,
            DrawLayer.BOUNDARIES,
            DrawLayer.RIVERS_AND_ISLANDS,
            DrawLayer.TEN_DASH_LINE,
            DrawLayer.LABELS,
            DrawLayer.BUSINESS_OVERLAYS
        ),
        plan.layerOrder
    )
}

@Test
fun visitedStateChangesStyleButNotGeometryIdentity() {
    val before = planFor(visited = emptySet())
    val after = planFor(visited = setOf("330000"))
    assertSame(before.feature("330000").pathKey, after.feature("330000").pathKey)
    assertNotEquals(before.feature("330000").fill, after.feature("330000").fill)
}
```

- [ ] **Step 3: Run render-plan tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.vector.VectorRenderPlanTest' --quiet`

Expected: FAIL because the plan and renderer do not exist.

- [ ] **Step 4: Implement the renderer without moving product chrome**

`ChinaMapView` keeps the existing gesture layer, viewport, marker/photo/location draw code, and native Compose overlays. When a ready vector scene is present and the migration flag is enabled, it delegates base-map and administrative geometry to `VectorMapRenderer`; otherwise it executes the existing drawing branch.

```kotlin
if (renderState.vectorEngineEnabled && renderState.vectorScene != null) {
    vectorRenderer.draw(
        drawScope = this,
        scene = renderState.vectorScene,
        viewport = viewport,
        style = MapStyleSheet.forTheme(renderState.backgroundTheme),
        businessState = renderState.toVectorBusinessState()
    )
} else {
    drawLegacyMap(renderState, viewport, pathCache)
}
```

The renderer draws fills once, strokes once, and labels last. National halo is visible only at national zoom. Ten-dash lines are independent round-cap strokes. No label or path allocation occurs inside a frame after cache warm-up.

- [ ] **Step 5: Run unit/build gates**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.vector.*' --tests 'com.mapchina.map.*' :shared:compileKotlinIosSimulatorArm64 --quiet`

Expected: PASS.

- [ ] **Step 6: Install and verify the migration renderer on Android**

Run:

```bash
./gradlew installDebug
adb shell monkey -p com.mapchina.app -c android.intent.category.LAUNCHER 1
sleep 3
adb shell input swipe 540 1100 700 900 500
adb shell input tap 930 420
adb shell input swipe 650 900 450 1050 500
adb exec-out screencap -p > .superpowers/sdd/offline-vector-map/android-national-after.png
adb logcat -d -t 600 | rg 'FATAL EXCEPTION|ANR in|MapChina|VectorMap'
```

Expected: the map is nonblank; China and surrounding land are correctly framed; near-white unvisited and jade visited provinces are distinct; national/province boundaries, sparse graticules, labels, islands, and all ten segments are visible at their intended zoom; markers and floating controls do not move or overlap.

- [ ] **Step 7: Build, install, and verify on iPhone 17 Pro simulator**

Use the project-discovered scheme and workspace/project command from the pre-change record. Install with `xcrun simctl install`, launch with `xcrun simctl launch`, perform equivalent pan/zoom/tap interactions, and save `.superpowers/sdd/offline-vector-map/ios-national-after.png`. Inspect the simulator log for crashes and package-loader failures.

- [ ] **Step 8: Commit national rendering**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map shared/src/commonTest/kotlin/com/mapchina/map
git commit -m "feat(map): render offline national vector scene"
```

---

### Task 11: Connect Footprints, Selection, Gestures, And Theme Settings

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapVectorIntegrationTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt`

**Interfaces:**
- ViewModel maps footprint and selection state to feature IDs; it does not rebuild geometry JSON.
- Controller hit testing uses scene indexes when the vector engine is active.
- Theme switching updates only the style and persists `MapTheme.name`.

- [ ] **Step 1: Add failing integration tests**

```kotlin
@Test
fun footprintRefreshUpdatesVisitedIdsWithoutReloadingScene() = runTest {
    viewModel.attachMapController(controller)
    sceneRepository.loadNational()
    val loadedScene = controller.renderState.value.vectorScene

    footprintRepository.add(provinceFootprint("330000"))
    viewModel.refresh()

    assertSame(loadedScene, controller.renderState.value.vectorScene)
    assertTrue("330000" in controller.renderState.value.visitedRegionIds)
}

@Test
fun missingThemeSettingAppliesStandardTheme() {
    viewModel.attachMapController(controller)
    assertEquals(MapTheme.STANDARD, controller.renderState.value.backgroundTheme)
}

@Test
fun holeAwareSceneHitRoutesThroughExistingRegionCallback() {
    controller.setVectorScene(sceneWithHole)
    controller.onTap(screenPointInsideOuter)
    assertEquals("330000", tappedRegionId)
    tappedRegionId = null
    controller.onTap(screenPointInsideHole)
    assertNull(tappedRegionId)
}
```

- [ ] **Step 2: Run integration tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.map.MapVectorIntegrationTest' --tests 'com.mapchina.ui.map.MapViewModelTest' --quiet`

Expected: FAIL because the ViewModel does not own a vector scene repository.

- [ ] **Step 3: Inject and connect the scene repository**

Load national data once when the map controller attaches. Maintain `visitedRegionIds`, `selectedRegionId`, and label priorities in `RenderState`. Keep old overlays synchronized only while the migration fallback is enabled.

```kotlin
private fun syncVectorBusinessState(controller: MapController) {
    controller.setVectorBusinessState(
        visitedRegionIds = regions.value.filter { it.visited }.mapTo(mutableSetOf()) { it.region.id },
        selectedRegionId = selectedRegion.value?.region?.id,
        footprintLabelIds = photoClusters.value.mapTo(mutableSetOf()) { it.regionId }
    )
}
```

Keep current double-tap zoom, pan, long-press, locate, camera restore, share mode, marker taps, and attraction preview callbacks. `MapTheme.entries` automatically exposes the standard theme in Profile; do not add explanatory copy.

- [ ] **Step 4: Run integration and full shared tests**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.map.*' --tests 'com.mapchina.map.*' --quiet && ./gradlew :shared:allTests --quiet`

Expected: PASS.

- [ ] **Step 5: Repeat Android and iOS interaction evidence**

On both platforms verify: cold start, one pan, pinch/double-tap zoom, province tap, back to national, locate, a marker tap, share mode, standard-to-water-ink theme switch and back, app relaunch retaining the saved theme. Save screenshots to `.superpowers/sdd/offline-vector-map/android-business-after.png` and `.superpowers/sdd/offline-vector-map/ios-business-after.png`; update `verification.md` with commands and observed results.

- [ ] **Step 6: Commit business integration**

```bash
git add shared/src/commonMain/kotlin/com/mapchina shared/src/commonTest/kotlin/com/mapchina
git commit -m "feat(map): connect footprints to vector scene"
```

---

### Task 12: Migrate Province, City, And District Drill-Down

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/vector/VectorMapSceneRepository.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapVectorDrillDownTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/map/vector/VectorMapSceneRepositoryTest.kt`

**Interfaces:**
- Province taps load city packs; city taps load district packs; back restores cached parents.
- Existing breadcrumbs, camera fit, selection, markers, and bottom-panel behavior remain unchanged.
- A loading or corrupt child pack never blanks or partially replaces the current map.

- [ ] **Step 1: Add failing end-to-end state tests**

```kotlin
@Test
fun nationalProvinceCityDistrictAndBackUseAtomicScenes() = runTest {
    viewModel.onRegionTapped("330000")
    completePack("cities/330000.topo")
    assertEquals(MapZoomLevel.PROVINCE, viewModel.currentLevel.value)

    viewModel.onRegionTapped("330100")
    completePack("districts/330100.topo")
    assertEquals(MapZoomLevel.CITY, viewModel.currentLevel.value)

    viewModel.navigateUp()
    assertEquals("330000", readyScene().scopeId)
    viewModel.navigateUp()
    assertEquals(VectorLayerScope.NATIONAL, readyScene().scope)
}

@Test
fun childFailureKeepsParentInteractiveAndPathUnchanged() = runTest {
    val before = readyNationalScene()
    failPack("cities/330000.topo", PackageFailure.HASH_MISMATCH)
    viewModel.onRegionTapped("330000")

    assertSame(before, controller.renderState.value.vectorScene)
    assertTrue(viewModel.currentPath.value.isEmpty())
    assertNotNull(viewModel.mapLoadError.value)
}
```

- [ ] **Step 2: Run drill-down tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.map.MapVectorDrillDownTest' --tests 'com.mapchina.map.vector.VectorMapSceneRepositoryTest' --quiet`

Expected: FAIL because existing drill-down still loads raw boundary strings.

- [ ] **Step 3: Route hierarchy transitions through scene loading**

Do not mutate `_currentLevel` or `_currentPath` until the requested child scene reaches `Ready`. Keep the parent scene and interaction enabled during loading. On success, atomically update scene, level, path, labels, hit index, and camera target. On failure, retain parent state and expose the existing recoverable error surface.

Use package feature bounds for camera fitting and package label anchors for labels. Remove runtime `BoundaryParser` work from the successful vector path, but retain it for the explicit fallback until Task 13.

- [ ] **Step 4: Run drill-down, map, and full shared tests**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.map.*' --tests 'com.mapchina.map.*' --quiet && ./gradlew :shared:allTests --quiet`

Expected: PASS.

- [ ] **Step 5: Verify deep navigation on Android and iOS**

On both platforms exercise:

1. National to Zhejiang.
2. Zhejiang to Hangzhou.
3. Tap one district and open its existing region surface.
4. Return twice to national.
5. Repeat with Hainan and Taiwan to inspect detached polygons and islands.
6. Trigger a controlled missing child pack in a debug build and confirm the parent does not blank.

Run the Android Iron Law commands, capture `.superpowers/sdd/offline-vector-map/android-drilldown-after.png`, capture the equivalent iOS screenshot, and inspect both crash logs.

- [ ] **Step 6: Commit lazy drill-down**

```bash
git add shared/src/commonMain/kotlin/com/mapchina shared/src/commonTest/kotlin/com/mapchina
git commit -m "feat(map): migrate drill-down to vector packs"
```

---

### Task 13: Switch The Default, Remove Legacy Data, And Prove Performance

**Files:**
- Delete: `androidApp/src/main/assets/boundaries/**`
- Delete: `androidApp/src/main/assets/districts/**`
- Delete: `iosApp/iosApp/Resources/boundaries/**`
- Delete: `iosApp/iosApp/Resources/districts/**`
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj`
- Delete: `shared/src/commonMain/kotlin/com/mapchina/map/NeighborOutlines.kt`
- Delete: `shared/src/commonMain/kotlin/com/mapchina/data/remote/BoundaryLoader.kt`
- Delete: `shared/src/androidMain/kotlin/com/mapchina/data/remote/BoundaryLoader.kt`
- Delete: `shared/src/iosMain/kotlin/com/mapchina/data/remote/BoundaryLoader.kt`
- Delete: `androidApp/src/main/assets/attractions.json`
- Delete: `androidApp/src/main/assets/attraction_details.json`
- Delete: `iosApp/iosApp/Resources/attractions.json`
- Delete: `iosApp/iosApp/Resources/attraction_details.json`
- Create: `shared/src/commonMain/composeResources/files/seeds/attractions.json`
- Create: `shared/src/commonMain/composeResources/files/seeds/attraction_details.json`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/remote/AttractionSeedResourceLoader.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/data/remote/AttractionSeedResourceLoaderTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/data/remote/DataSeeder.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`
- Modify: `shared/src/androidMain/kotlin/com/mapchina/di/AndroidModule.kt`
- Modify: `shared/src/iosMain/kotlin/com/mapchina/ios/MainKt.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/vector/LegacyMapRemovalTest.kt`
- Create: `docs/map-review/README.md`
- Create: `docs/map-review/data-provenance.json`
- Create: `docs/map-review/visual-checklist.md`

**Completion gate:** The legacy renderer and duplicated assets are removed only after Tasks 10-12 have fresh dual-platform evidence. Formal public release remains blocked until the review number is real.

- [ ] **Step 1: Add failing legacy-removal and package-size checks**

```kotlin
@Test
fun productionRenderStateAlwaysUsesVectorEngine() {
    val state = RenderState()
    assertTrue(state.vectorEngineEnabled)
    assertFalse(state.hasLegacyNeighborOutlines)
}
```

Add a Gradle verification task that fails if release inputs contain `assets/boundaries`, `assets/districts`, iOS copied equivalents, `NeighborOutlines`, or more than one `map-package/manifest.json`.

- [ ] **Step 2: Remove duplicate resources and fallback code**

Move the two attraction seed files to shared Compose Resources before removing the Xcode `Copy Map Data Resources` phase. `AttractionSeedResourceLoader` must parse the same `AttractionSeed` records and image URL map from `Res.readBytes("files/seeds/...")`; add a parity test proving the shared loader returns the same attraction count, IDs, coordinates, and image URLs as the current platform files. Replace boundary seeding with package metadata or retain only non-geometry region metadata in SQL. Remove `setNeighborOutlines`, legacy hit-test coordinate maps, legacy path construction, and the migration flag. Keep old unit tests only where they still cover a used geometry utility; delete tests for dead paths in the same commit.

- [ ] **Step 3: Run all deterministic quality gates**

Run:

```bash
./gradlew :mapFormat:allTests :mapCompiler:test :shared:allTests :server:test :androidApp:testDebugUnitTest --quiet
./gradlew verifyVectorMapPackage
./gradlew clean installDebug
du -sk shared/src/commonMain/composeResources/files/map-package
find androidApp iosApp -type d \( -name boundaries -o -name districts \)
rg -n 'NeighborOutlines|BoundaryLoader|vectorEngineEnabled = false' shared androidApp iosApp
```

Expected: all tests/builds PASS; package size is at most 26 MiB; the `find` and `rg` checks return no legacy map data/runtime matches.

- [ ] **Step 4: Run final Android visual and interaction proof**

Run:

```bash
adb shell monkey -p com.mapchina.app -c android.intent.category.LAUNCHER 1
sleep 3
adb shell input swipe 540 1100 740 850 600
adb shell input tap 600 740
adb shell input swipe 540 1100 360 900 600
adb exec-out screencap -p > .superpowers/sdd/offline-vector-map/android-final.png
adb shell dumpsys gfxinfo com.mapchina.app reset
```

Record a 10-second scripted national pan/zoom loop, then run `adb shell dumpsys gfxinfo com.mapchina.app` and capture a Perfetto or Compose trace. Expected: no crash/ANR, no blank frames, P95 frame time at most 16.7 ms, and no frame above 50 ms on Pixel 9 Pro.

- [ ] **Step 5: Run final iOS visual and performance proof**

Clean-build, install, and launch on iPhone 17 Pro simulator. Repeat the same national and drill-down flow, capture `.superpowers/sdd/offline-vector-map/ios-final.png`, and collect Time Profiler plus Animation Hitches evidence for a 10-second pan/zoom loop. Expected: no package error, no blank scene, stable labels, no content jump during child loading, and the same layer order/colors/camera framing as Android.

- [ ] **Step 6: Measure startup and memory budgets**

Instrument loader start, first vector frame, and scene-ready timestamps using the existing `PerformanceTrace` boundary. On Android record `dumpsys meminfo` at national and deepest city scope. On iOS record the corresponding Instruments allocation footprint.

Expected:

- First vector frame at most 750 ms after loader start.
- National incremental working set at most 40 MiB.
- Deepest single-city working set at most 75 MiB.
- If a threshold fails, optimize compiler LOD/index/caches and repeat both platforms; do not remove approved layers.

- [ ] **Step 7: Assemble review material without claiming approval**

`docs/map-review/README.md` must state package data version, build commit, reference standard-map number `GS(2023)2767`, package source hashes, map bounds, style changes, business overlays, and submission steps. `visual-checklist.md` must include national, Northeast, Northwest, Tibet/Xinjiang, Hong Kong/Macao, Taiwan/east segment, Hainan/South China Sea, and surrounding-region crops. Leave `reviewNumber` null until the authority returns a real number; updating it requires rebuilding and re-running package hash tests.

- [ ] **Step 8: Request final code review and fix findings**

Use `superpowers:requesting-code-review` across the complete change range. Resolve all correctness, data-loss, lifecycle, memory, accessibility, and cross-platform findings, then rerun Steps 3-6.

- [ ] **Step 9: Commit the production switch**

```bash
git add -A androidApp iosApp shared mapFormat mapCompiler map-data docs/map-review
git status --short
git commit -m "feat(map): ship offline vector map engine"
```

Expected: `git status --short` before commit contains only the intended engine/data/evidence files plus the preserved unrelated untracked haptic plan, which must not be staged.

---

## Final Acceptance Checklist

- [ ] Default home map is self-rendered vector geometry with no bitmap basemap, map SDK, or network request.
- [ ] China and surrounding land, ocean, national/province/city/district boundaries, major rivers, important islands, sparse graticules, labels, and all ten South China Sea segments render by zoom policy.
- [ ] Unvisited provinces are near-white; visited provinces are low-saturation jade; selection is clear without turning the map into a heatmap.
- [ ] Multipolygons, detached islands, and holes render and hit-test correctly.
- [ ] Province/city/district packs load lazily without blanking, jumping, or partial scene replacement.
- [ ] Footprints, photos, attractions, locate, share mode, gestures, camera restore, navigation, and existing non-standard themes have no regression.
- [ ] One shared Compose resource package replaces both platform data copies and is at most 26 MiB.
- [ ] Android Pixel 9 Pro and iPhone 17 Pro simulator pass visual, interaction, log, startup, frame-time, and memory gates with saved before/after evidence.
- [ ] Public release remains blocked until modified-map review is complete and the real review number is compiled into the manifest.
