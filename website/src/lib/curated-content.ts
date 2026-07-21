import type { AttractionViewModel, SiteLocale, StoryViewModel } from '@/types';

type AttractionSeed = Pick<AttractionViewModel, 'slug' | 'name' | 'region' | 'description' | 'image'>;
type StorySeed = Pick<StoryViewModel, 'slug' | 'author' | 'title' | 'excerpt' | 'content' | 'coverImage' | 'region'>;

const attractionSeeds: Record<SiteLocale, AttractionSeed[]> = {
  zh: [
    { slug: 'beijing-central-axis', name: '北京中轴线', region: '北京', description: '从钟鼓楼到永定门，沿城市脊梁阅读古都秩序。', image: null },
    { slug: 'hangzhou-west-lake', name: '西湖环线', region: '浙江 · 杭州', description: '在山水与城市之间小驻，感受十景之外的日常。', image: null },
    { slug: 'dunhuang-mogao', name: '敦煌莫高窟', region: '甘肃 · 敦煌', description: '循着丝路抵达壁画与洞窟交织的千年现场。', image: null },
    { slug: 'yunnan-yuanyang', name: '元阳梯田', region: '云南 · 红河', description: '在云海与水田的季节变化中，看见山地生活。', image: null },
    { slug: 'quanzhou-old-city', name: '泉州古城', region: '福建 · 泉州', description: '沿着香火、骑楼和港口遗迹，重读海上丝路。', image: null },
    { slug: 'qinghai-lake', name: '青海湖', region: '青海', description: '让高原的风、湖面与远山重新校准旅行的速度。', image: null },
    { slug: 'suzhou-gardens', name: '苏州园林', region: '江苏 · 苏州', description: '在咫尺山林里，观察窗、石、水与四季的关系。', image: null },
    { slug: 'guilin-li-river', name: '漓江山水', region: '广西 · 桂林', description: '顺流而下，让喀斯特峰林展开一幅移动的长卷。', image: null },
    { slug: 'xian-city-wall', name: '西安城墙', region: '陕西 · 西安', description: '从城墙高度重新认识街巷、城门与古都边界。', image: null },
  ],
  en: [
    { slug: 'beijing-central-axis', name: 'Beijing Central Axis', region: 'Beijing', description: 'Walk the historic spine of the capital from the Drum Tower to Yongdingmen.', image: null },
    { slug: 'hangzhou-west-lake', name: 'West Lake Loop', region: 'Hangzhou · Zhejiang', description: 'Pause between landscape and city life along the shores of West Lake.', image: null },
    { slug: 'dunhuang-mogao', name: 'Mogao Caves', region: 'Dunhuang · Gansu', description: 'Follow the Silk Road into a millennium of murals and cave art.', image: null },
    { slug: 'yunnan-yuanyang', name: 'Yuanyang Rice Terraces', region: 'Honghe · Yunnan', description: 'See mountain life reflected in seasonal clouds and flooded terraces.', image: null },
    { slug: 'quanzhou-old-city', name: 'Quanzhou Old City', region: 'Quanzhou · Fujian', description: 'Trace the Maritime Silk Road through temples, arcades, and port memories.', image: null },
    { slug: 'qinghai-lake', name: 'Qinghai Lake', region: 'Qinghai', description: 'Let highland wind, open water, and distant mountains reset your pace.', image: null },
    { slug: 'suzhou-gardens', name: 'Suzhou Gardens', region: 'Suzhou · Jiangsu', description: 'Read the relationship between windows, stone, water, and seasons.', image: null },
    { slug: 'guilin-li-river', name: 'Li River', region: 'Guilin · Guangxi', description: 'Drift through karst peaks as the landscape unfolds like a moving scroll.', image: null },
    { slug: 'xian-city-wall', name: 'Xi’an City Wall', region: 'Xi’an · Shaanxi', description: 'See streets, gates, and the old capital boundary from the wall above.', image: null },
  ],
};

const storySeeds: Record<SiteLocale, StorySeed[]> = {
  zh: [
    { slug: 'autumn-central-axis', author: 'MapChina 编辑部', title: '沿着中轴线，走进北京的秋天', excerpt: '从晨钟到城门，把古都的一天交给脚步。', content: '清晨从钟鼓楼出发，屋脊和树影把街巷切成明暗两半。一路向南，城市的尺度在步行中慢慢显现。\n\n抵达永定门时，天色已经变暖。这条路不是景点清单，而是一条可以反复走进的城市脊梁。', coverImage: null, region: '北京' },
    { slug: 'west-lake-rain', author: '青禾', title: '雨后的西湖，适合慢一点', excerpt: '避开人潮，在湿润的石阶与桂香之间小驻。', content: '雨停后，湖面把远山压成很淡的一层灰。沿着杨公堤慢慢走，不必追赶十景，也能遇见属于自己的片刻。', coverImage: null, region: '浙江 · 杭州' },
    { slug: 'dunhuang-night', author: '远山来信', title: '敦煌入夜之后', excerpt: '白日看洞窟，夜里听风从沙丘上经过。', content: '傍晚离开洞窟，壁画里的颜色仍留在眼前。沙漠降温很快，风把白天的喧闹一点点带走。', coverImage: null, region: '甘肃 · 敦煌' },
    { slug: 'yuanyang-clouds', author: '阿野', title: '云落在元阳梯田', excerpt: '水面收下天光，也收下山地生活的节奏。', content: '日出前的梯田没有鲜艳颜色，只有云和水面交换着微弱的光。等村庄醒来，山坡也跟着有了声音。', coverImage: null, region: '云南 · 红河' },
    { slug: 'quanzhou-incense', author: '一川', title: '在泉州，跟着香火走', excerpt: '庙宇、骑楼与港口记忆，藏在步行可达的日常里。', content: '古城的路并不宽，香火气却能把不同方向连在一起。转过街角，海上丝路不再是遥远名词，而是仍在生活里的痕迹。', coverImage: null, region: '福建 · 泉州' },
    { slug: 'qinghai-wind', author: '北纬三十六', title: '把一天留给青海湖的风', excerpt: '不赶路的时候，湖面才显出真正的尺度。', content: '远山看起来很近，走起来却总有新的距离。高原的风让所有声音变得简单，也让停留本身成为旅程。', coverImage: null, region: '青海' },
    { slug: 'suzhou-window', author: '檐下', title: '从一扇窗看苏州', excerpt: '园林的远近，不由地图决定，而由借景决定。', content: '穿过月洞门，上一处景色还没有结束，下一处已经从窗框里出现。园林把行走变成一种缓慢的观看。', coverImage: null, region: '江苏 · 苏州' },
    { slug: 'li-river-morning', author: '舟行', title: '漓江清晨的第一班船', excerpt: '薄雾把峰林留白，水面替旅程写下开头。', content: '天刚亮时，峰林还没有完整轮廓。船向前，雾慢慢退开，一座座山像从纸面上浮出来。', coverImage: null, region: '广西 · 桂林' },
  ],
  en: [
    { slug: 'autumn-central-axis', author: 'MapChina Editors', title: 'Autumn Along Beijing’s Central Axis', excerpt: 'Walk from morning bells to the southern gate and let the city set the pace.', content: 'Start at the Drum and Bell Towers while the lanes are still divided by roof shadows. Walking south reveals the scale of the capital one block at a time.\n\nBy Yongdingmen, the light has warmed. This is less a checklist than a historic spine worth returning to.', coverImage: null, region: 'Beijing' },
    { slug: 'west-lake-rain', author: 'Qinghe', title: 'West Lake After the Rain', excerpt: 'Slow down between wet stone paths, osmanthus, and distant hills.', content: 'After the rain, the lake presses the mountains into a pale grey line. Walk the Yanggong Causeway without chasing landmarks and the quieter lake will find you.', coverImage: null, region: 'Hangzhou · Zhejiang' },
    { slug: 'dunhuang-night', author: 'Letters from Afar', title: 'When Night Reaches Dunhuang', excerpt: 'See the caves by day, then hear the wind crossing the dunes.', content: 'The colors of the murals remain after leaving the caves. The desert cools quickly, and the evening wind carries away the noise of the day.', coverImage: null, region: 'Dunhuang · Gansu' },
    { slug: 'yuanyang-clouds', author: 'Aye', title: 'Clouds Over Yuanyang', excerpt: 'Terraced water holds the sky and the rhythm of mountain life.', content: 'Before sunrise the terraces have little color, only clouds and water exchanging faint light. When the villages wake, the slopes begin to make sound.', coverImage: null, region: 'Honghe · Yunnan' },
    { slug: 'quanzhou-incense', author: 'Yichuan', title: 'Following Incense Through Quanzhou', excerpt: 'Temples, arcades, and port memories remain part of the walkable city.', content: 'The old streets are narrow, yet incense connects places in every direction. Around each corner, the Maritime Silk Road feels less like history and more like daily life.', coverImage: null, region: 'Quanzhou · Fujian' },
    { slug: 'qinghai-wind', author: 'Latitude 36', title: 'A Day with the Wind at Qinghai Lake', excerpt: 'The lake reveals its true scale when you stop rushing.', content: 'The mountains look close, but every walk creates new distance. Highland wind simplifies every sound and turns staying still into part of the journey.', coverImage: null, region: 'Qinghai' },
    { slug: 'suzhou-window', author: 'Under the Eaves', title: 'Suzhou Through a Garden Window', excerpt: 'Distance in a garden is framed by borrowed scenery, not a map.', content: 'Pass through a moon gate and the previous scene has not ended before the next appears inside a window. The garden turns walking into slow observation.', coverImage: null, region: 'Suzhou · Jiangsu' },
    { slug: 'li-river-morning', author: 'River Passage', title: 'The First Boat on the Li River', excerpt: 'Mist leaves the peaks unfinished while the water begins the journey.', content: 'At first light the peaks have no complete outline. As the boat moves and the mist lifts, each mountain seems to rise from paper.', coverImage: null, region: 'Guilin · Guangxi' },
  ],
};

export function getEditorialAttractions(locale: SiteLocale): AttractionViewModel[] {
  return attractionSeeds[locale].map((seed) => ({
    ...seed,
    id: `editorial-attraction-${seed.slug}`,
    source: 'editorial',
    level: null,
    visitCount: null,
    coordinates: null,
  }));
}

export function getEditorialStories(locale: SiteLocale): StoryViewModel[] {
  return storySeeds[locale].map((seed) => ({
    ...seed,
    id: `editorial-story-${seed.slug}`,
    source: 'editorial',
    likeCount: null,
    commentCount: null,
    createdAt: null,
  }));
}
