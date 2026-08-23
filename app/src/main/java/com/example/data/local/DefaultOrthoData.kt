package com.example.data.local

import com.example.data.local.entity.BoneEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.ImageEntity
import com.example.data.local.entity.SectionEntity

object DefaultOrthoData {

    val chapters = listOf(
        ChapterEntity(
            id = "chap_upper_limb",
            title = "Upper Extremity Trauma",
            description = "Clavicle, shoulder girdle, humerus, forearm and hand fracture fixation",
            icon = "healing",
            orderIndex = 1,
            regionTag = "Upper Limb"
        ),
        ChapterEntity(
            id = "chap_lower_limb",
            title = "Lower Extremity Trauma",
            description = "Pelvis, femur, patella, tibia, and foot/ankle operative guides",
            icon = "accessibility_new",
            orderIndex = 2,
            regionTag = "Lower Limb"
        ),
        ChapterEntity(
            id = "chap_spine",
            title = "Spine & Pelvic Ring",
            description = "Subaxial cervical, thoracolumbar instrumentation, and pelvic ring disruption",
            icon = "view_agenda",
            orderIndex = 3,
            regionTag = "Spine"
        ),
        ChapterEntity(
            id = "chap_pediatric",
            title = "Pediatric Orthopedics",
            description = "Supracondylar humerus, pediatric femur ESIN, SCFE pinning",
            icon = "child_care",
            orderIndex = 4,
            regionTag = "Pediatric"
        ),
        ChapterEntity(
            id = "chap_arthroplasty",
            title = "Reconstruction & Arthroplasty",
            description = "Primary THA, TKA principles, approaches, balance and alignment",
            icon = "medical_services",
            orderIndex = 5,
            regionTag = "Arthroplasty"
        ),
        ChapterEntity(
            id = "chap_sports",
            title = "Sports & Arthroscopy",
            description = "ACL reconstruction, meniscus repair, shoulder instability",
            icon = "sports_kabaddi",
            orderIndex = 6,
            regionTag = "Sports"
        )
    )

    val bones = listOf(
        // Upper Limb
        BoneEntity(
            id = "bone_distal_radius",
            chapterId = "chap_upper_limb",
            name = "Distal Radius Fractures",
            subtitle = "Volar Locking Plate, FCR Approach & Radial Column Support",
            region = "Wrist & Forearm",
            traumaType = "Extra / Intra-articular (AO 23-A to C)",
            classificationSummary = "Frykman, AO/OTA, Fernandez (Bending, Shear, Compression, Avulsion, High Energy)",
            orderIndex = 1,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_clavicle",
            chapterId = "chap_upper_limb",
            name = "Clavicle Fractures",
            subtitle = "Superior vs Anterior Plating & Neurovascular Protection",
            region = "Shoulder Girdle",
            traumaType = "Midshaft & Distal Fractures (Allman Group I-III, Neer Type II)",
            classificationSummary = "Allman, Robinson, Neer Distal Clavicle Classification",
            orderIndex = 2,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_proximal_humerus",
            chapterId = "chap_upper_limb",
            name = "Proximal Humerus Fractures",
            subtitle = "Deltopectoral vs Deltoid Split, Calcar Screws & Tuberosity Control",
            region = "Shoulder",
            traumaType = "2, 3, 4-Part Fractures & Fracture-Dislocations (AO 11-A to C)",
            classificationSummary = "Neer Classification (Parts: Greater Tuberosity, Lesser Tuberosity, Head, Shaft)",
            orderIndex = 3,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_humeral_shaft",
            chapterId = "chap_upper_limb",
            name = "Humeral Shaft Fractures",
            subtitle = "Antegrade / Retrograde IM Nailing vs Open Plating & Radial Nerve Care",
            region = "Arm",
            traumaType = "Transverse, Oblique, Spiral, Segmental (AO 12-A to C)",
            classificationSummary = "Holstein-Lewis fracture of distal 1/3 at risk for radial nerve entrapment",
            orderIndex = 4,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_distal_humerus",
            chapterId = "chap_upper_limb",
            name = "Distal Humerus (Bicolumnar)",
            subtitle = "Olecranon Osteotomy, Parallel vs Orthogonal Plating & Ulnar Transposition",
            region = "Elbow",
            traumaType = "Intercondylar, Intra-articular Bicolumnar Fractures (AO 13-C)",
            classificationSummary = "AO/OTA 13-A (Extra-articular), 13-B (Partial articular), 13-C (Complete articular)",
            orderIndex = 5,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_forearm_shaft",
            chapterId = "chap_upper_limb",
            name = "Forearm Diaphyseal Fractures",
            subtitle = "Both Bones, Galeazzi & Monteggia - Anatomical Restoration of Radial Bow",
            region = "Forearm",
            traumaType = "Radius & Ulna Shaft Fractures, Monteggia (Bado), Galeazzi",
            classificationSummary = "Bado Classification (Types I-IV based on radial head dislocation direction)",
            orderIndex = 6,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_scaphoid",
            chapterId = "chap_upper_limb",
            name = "Scaphoid Fractures",
            subtitle = "Percutaneous Volar / Dorsal Headless Screws & Retrograde Vascular Risk",
            region = "Wrist",
            traumaType = "Waist, Proximal Pole & Distal Tubercle Fractures",
            classificationSummary = "Herbert Classification (A: Stable, B: Unstable Acute, C: Delayed, D: Nonunion)",
            orderIndex = 7,
            isHighYield = true
        ),

        // Lower Limb
        BoneEntity(
            id = "bone_femoral_neck",
            chapterId = "chap_lower_limb",
            name = "Femoral Neck & Intertrochanteric",
            subtitle = "Cephalomedullary Nailing, Dynamic Hip Screw & Tip-Apex Distance Rule",
            region = "Hip & Proximal Femur",
            traumaType = "Intracapsular (Garden I-IV) & Extracapsular Pertrochanteric (AO 31-A)",
            classificationSummary = "Garden Classification (I: Incomplete, II: Complete non-displaced, III: Partial, IV: Complete) & Pauwels",
            orderIndex = 8,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_femoral_shaft",
            chapterId = "chap_lower_limb",
            name = "Femoral Shaft Fractures",
            subtitle = "Antegrade Reamed Nailing, Entry Point Selection (Troch vs Piriformis)",
            region = "Thigh",
            traumaType = "Diaphyseal Femur Fractures (AO 32-A to C, Winquist-Hansen)",
            classificationSummary = "Winquist-Hansen Classification (Type 0: No comminution to Type IV: Segmental loss)",
            orderIndex = 9,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_distal_femur",
            chapterId = "chap_lower_limb",
            name = "Distal Femur & Hoffa Fractures",
            subtitle = "Lateral Locked Plating, Retrograde Nailing & Dual Plating Strategies",
            region = "Distal Femur & Knee",
            traumaType = "Supracondylar & Intercondylar Fractures (AO 33-A to C, Hoffa B3)",
            classificationSummary = "AO/OTA 33-A (Extra-articular), 33-B (Unicondylar/Hoffa), 33-C (Complete Bicondylar)",
            orderIndex = 10,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_patella",
            chapterId = "chap_lower_limb",
            name = "Patella Fractures",
            subtitle = "Modified Tension Band Wiring, Cannulated Screws & Rim Cerclage",
            region = "Knee",
            traumaType = "Transverse, Stellate, Comminuted, Inferior Pole Avulsion",
            classificationSummary = "Descriptive: Transverse, Vertical, Marginal, Comminuted, Osteochondral",
            orderIndex = 11,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_tibial_plateau",
            chapterId = "chap_lower_limb",
            name = "Tibial Plateau Fractures",
            subtitle = "Schatzker I-VI, Subchondral Raft Screws & Dual Incision Strategy",
            region = "Proximal Tibia & Knee",
            traumaType = "Split, Depression, Bicondylar, Fracture-Dislocation (AO 41-B to C)",
            classificationSummary = "Schatzker Classification (I: Lat split, II: Lat split-depression, III: Pure depression, IV: Medial, V: Bicondylar, VI: Metadiaphyseal dissociation)",
            orderIndex = 12,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_tibial_shaft",
            chapterId = "chap_lower_limb",
            name = "Tibial Shaft & Compartment Syndrome",
            subtitle = "Suprapatellar vs Infrapatellar IM Nailing & Emergency 4-Compartment Release",
            region = "Leg",
            traumaType = "Diaphyseal Tibia Fractures (AO 42-A to C) & Open Gustilo-Anderson",
            classificationSummary = "Gustilo-Anderson for Open Fractures (Type I <1cm, II 1-10cm, IIIA periosteal cover, IIIB periosteal stripping, IIIC vascular repair)",
            orderIndex = 13,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_ankle_malleoli",
            chapterId = "chap_lower_limb",
            name = "Ankle Malleoli & Syndesmosis",
            subtitle = "Danis-Weber, Lauge-Hansen Vectors, Posterior Malleolus & Syndesmotic Fixation",
            region = "Ankle",
            traumaType = "Bimalleolar, Trimalleolar, Maisonneuve, Pilon (AO 44-A to C)",
            classificationSummary = "Danis-Weber (A: Infrasyndesmotic, B: Transsyndesmotic, C: Suprasyndesmotic) & Lauge-Hansen (SER, PER, SA, PA)",
            orderIndex = 14,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_calcaneus",
            chapterId = "chap_lower_limb",
            name = "Calcaneus & Talus Fractures",
            subtitle = "Extensile Lateral vs Sinus Tarsi, Bohler Angle & Sural Nerve Sparing",
            region = "Foot & Hindfoot",
            traumaType = "Intra-articular Calcaneal & Talar Neck/Body Fractures (Sanders & Hawkins)",
            classificationSummary = "Sanders Classification (CT-based number of posterior facet articular fragments I-IV) & Hawkins Talar Neck (I-IV)",
            orderIndex = 15,
            isHighYield = true
        ),

        // Spine
        BoneEntity(
            id = "bone_cervical_spine",
            chapterId = "chap_spine",
            name = "Subaxial Cervical Trauma & ACDF",
            subtitle = "Smith-Robinson Approach, Recurrent Laryngeal Nerve & Corpectomy/Plating",
            region = "Cervical Spine",
            traumaType = "Facet Dislocation, Teardrop, Burst Fracture (SLIC Score)",
            classificationSummary = "Subaxial Injury Classification (SLIC): Morphology + Disco-ligamentous + Neurological status",
            orderIndex = 16,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_thoracolumbar",
            chapterId = "chap_spine",
            name = "Thoracolumbar Burst Fractures",
            subtitle = "Freehand & Fluoroscopic Pedicle Screws, TLICS Score & Decompression",
            region = "Thoracolumbar Spine",
            traumaType = "Compression, Burst, Chance, Fracture-Dislocation (AO Spine & TLICS)",
            classificationSummary = "TLICS System: Fracture morphology + Posterior Ligamentous Complex (PLC) integrity + Neurologic status (Score >= 5 operates)",
            orderIndex = 17,
            isHighYield = true
        ),

        // Pediatric
        BoneEntity(
            id = "bone_peds_supracondylar",
            chapterId = "chap_pediatric",
            name = "Pediatric Supracondylar Humerus",
            subtitle = "Gartland Classification, Closed Reduction Maneuvers & Divergent Lateral Pins",
            region = "Pediatric Elbow",
            traumaType = "Extension Type I-IV, Flexion Type",
            classificationSummary = "Modified Gartland (Type I: Non-displaced, Type II: Intact posterior cortex, Type III: Completely displaced, Type IV: Multidirectionally unstable)",
            orderIndex = 18,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_peds_femur",
            chapterId = "chap_pediatric",
            name = "Pediatric Femur Fractures (ESIN)",
            subtitle = "Elastic Stable Intramedullary Nails (Nancy Nails) & Retrograde Insertion",
            region = "Pediatric Thigh",
            traumaType = "Midshaft Diaphyseal Femur in children 5 to 11 years old",
            classificationSummary = "Treatment based on age: <6 mos Pavlik, 6 mos-5 yrs Spica cast, 5-11 yrs ESIN, >11 yrs Rigid intramedullary nail",
            orderIndex = 19,
            isHighYield = true
        ),

        // Arthroplasty
        BoneEntity(
            id = "bone_tha_approaches",
            chapterId = "chap_arthroplasty",
            name = "Total Hip Arthroplasty (THA)",
            subtitle = "Direct Anterior (Hueter) vs Posterior (Moore) & Lewinnek Safe Zone",
            region = "Hip Joint",
            traumaType = "Osteoarthritis, AVN, Femoral Neck Nonunion",
            classificationSummary = "Safe Zone: Cup Inclination 40° ± 10°, Cup Anteversion 15° ± 10°. Combined anteversion rule ~35°-45°",
            orderIndex = 20,
            isHighYield = true
        ),
        BoneEntity(
            id = "bone_tka_principles",
            chapterId = "chap_arthroplasty",
            name = "Total Knee Arthroplasty (TKA)",
            subtitle = "Measured Resection vs Gap Balancing, Joint Line & Medial Parapatellar",
            region = "Knee Joint",
            traumaType = "Tricompartmental Knee Osteoarthritis & Severe Deformity",
            classificationSummary = "Mechanical Axis Target: Hip-Knee-Ankle 180° ± 3°. Gap symmetry in 0° extension & 90° flexion",
            orderIndex = 21,
            isHighYield = true
        ),

        // Sports
        BoneEntity(
            id = "bone_acl_reconstruction",
            chapterId = "chap_sports",
            name = "ACL Reconstruction",
            subtitle = "BTB vs Hamstring vs Quad Tendon, Anteromedial Portal & Anatomical Footprint",
            region = "Knee Ligaments",
            traumaType = "Acute & Chronic Anterior Cruciate Ligament Rupture",
            classificationSummary = "Anatomical femoral footprint center: Posterior to resident ridge, 2.5-3mm anterior to posterior articular cartilage edge",
            orderIndex = 22,
            isHighYield = true
        )
    )

    val sections = listOf(
        // Distal Radius Sections
        SectionEntity(
            id = "sec_distal_radius_1",
            boneId = "bone_distal_radius",
            sectionType = "ANATOMY",
            title = "Anatomical Landmarks & Watershed Line",
            content = "The distal radius articulation consists of the scaphoid facet (triangular), lunate facet (quadrangular), and sigmoid notch. Normal parameters: Radial inclination (22°), Volar tilt (11°), Radial length/height (11-12mm), Ulnar variance (neutral ± 2mm).\n\nThe Watershed Line is a distinct transverse transverse ridge 2mm proximal to the radiocarpal joint line. Placing hardware distal to this line risks flexor pollicis longus (FPL) tendon attrition and rupture.",
            pearls = "Always palpate the volar rim; never place a plate distal to the watershed line.\nPreserve the pronator quadratus cuff for repair over the plate to cushion flexor tendons.",
            warnings = "Plates extending past the volar watershed line carry an 8x increased risk of late FPL tendon rupture.",
            orderIndex = 1
        ),
        SectionEntity(
            id = "sec_distal_radius_2",
            boneId = "bone_distal_radius",
            sectionType = "APPROACH",
            title = "Modified Henry Approach (Volar FCR Bed)",
            content = "1. Make an 8cm longitudinal incision directly over the Flexor Carpi Radialis (FCR) tendon sheath.\n2. Incise the anterior sheath of FCR and retract the tendon ulnarly (protects the median nerve and palmar cutaneous branch).\n3. Incise the posterior sheath of FCR to enter the deep Parona space.\n4. Identify the Pronator Quadratus (PQ) muscle. Make an 'L-shaped' incision along its radial and distal borders, leaving a 2mm cuff on the radius for closure.\n5. Subperiosteally elevate the PQ muscle ulnarly to expose the volar cortex of the distal radius.",
            pearls = "Keep dissection strictly in the FCR tendon sheath; going medial puts the palmar cutaneous branch of the median nerve at severe risk.\nNever retract vigorously with sharp hohmanns over the ulnar margin to avoid compressing the median nerve.",
            warnings = "The radial artery lies just lateral to the FCR sheath in the proximal wound; maintain careful lateral retraction.",
            orderIndex = 2
        ),
        SectionEntity(
            id = "sec_distal_radius_3",
            boneId = "bone_distal_radius",
            sectionType = "REDUCTION",
            title = "Reduction Techniques & Provisional Fixation",
            content = "1. Traction & Hyperextension / Hyperflexion: Disimpact dorsally tilted fragments by applying longitudinal traction, gentle hyperextension to free periosteal hinge, then volar translation and palmar flexion.\n2. Intrafocal K-Wire (Kapandji maneuver): Insert a 1.6mm or 2.0mm K-wire into the dorsal fracture line and lever the distal fragment into volar tilt.\n3. Radial Styloid Control: Place a 1.6mm K-wire through the radial styloid aimed proximal-ulnar to lock radial length and inclination.",
            pearls = "A provisional non-locking screw placed in the oblong shaft hole pulls the radial shaft down to the plate, automatically restoring volar tilt.\nVerify subchondral screw placement with a 30° elevated lateral fluoroscopic beam to ensure no intra-articular screw penetration.",
            warnings = "Do not accept >1mm articular step-off or dorsal tilt >0° in young active patients.",
            orderIndex = 3
        ),
        SectionEntity(
            id = "sec_distal_radius_4",
            boneId = "bone_distal_radius",
            sectionType = "FIXATION",
            title = "Volar Locking Plate Construct Strategy",
            content = "1. Position plate proximal to the watershed line.\n2. Insert a cortical screw into the oblong sliding slot on the diaphysis to achieve anatomical plate-bone contact and fine-tune proximal-distal position.\n3. Drill and place distal subchondral locking screws. Target the subchondral bone 2-4mm below the articular surface.\n4. Ensure separate fixation of the critical ulnar lunate facet (volar-medial fragment) which prevents carpal subluxation.\n5. Verify screw length on 20° tilted skyline/tangential views to avoid dorsal cortical screw protrusion and extensor tendon fraying (EPL/EDC).",
            pearls = "Use unicortical locking screws distally if bone quality is good to prevent dorsal tendon irritation.\nRepair the pronator quadratus with 3-0 braided absorbable sutures over the plate.",
            warnings = "Dorsal screw prominence >1.5mm is the #1 cause of post-op Extensor Pollicis Longus (EPL) rupture.",
            orderIndex = 4
        ),

        // Clavicle Sections
        SectionEntity(
            id = "sec_clavicle_1",
            boneId = "bone_clavicle",
            sectionType = "ANATOMY",
            title = "Clavicle Anatomy & Neurovascular Danger Corridor",
            content = "The clavicle is an S-shaped bone with an anterior convexity medially and anterior concavity laterally. The subclavian artery, subclavian vein, and brachial plexus trunks lie immediately posterior and inferior to the medial and middle thirds, separated only by the subclavius muscle and clavipectoral fascia.",
            pearls = "Drill strictly from anterior-to-posterior or superior-to-inferior with a guarded drill bit and a deep malleable retractor.\nUse oscillating drill mode when penetrating the far cortex adjacent to neurovascular structures.",
            warnings = "Plunging a drill bit past the inferior/posterior cortex risks fatal laceration of the subclavian vessels.",
            orderIndex = 1
        ),
        SectionEntity(
            id = "sec_clavicle_2",
            boneId = "bone_clavicle",
            sectionType = "FIXATION",
            title = "Superior vs Anterior Plating Techniques",
            content = "Superior Plating: Structurally biomechanically superior as it sits on the tension band surface. Allows longer screws directed inferiorly. More hardware prominence.\n\nAnterior Plating: Ideal for low soft-tissue profiles. Screws directed anterior-to-posterior (parallel to neurovascular structures, safer trajectory). Requires longer pre-contoured plates.\n\nConstruct Goals: Minimum 3 bicortical screws (6 cortices) on each side of the fracture. Use 3.5mm low-profile reconstruction or pre-contoured anatomical locking plates.",
            pearls = "For simple transverse or oblique fractures, interfragmentary lag screw placement followed by neutralization plating yields 98% union.\nPre-contour plates to match the clavicle's S-curve prior to rigid fixation.",
            warnings = "Identify and preserve supraclavicular sensory nerve branches to avoid anterior chest wall numbness or painful neuroma.",
            orderIndex = 2
        ),

        // Proximal Humerus Sections
        SectionEntity(
            id = "sec_proximal_humerus_1",
            boneId = "bone_proximal_humerus",
            sectionType = "APPROACH",
            title = "Deltopectoral vs Anterolateral Deltoid-Split Approaches",
            content = "Deltopectoral Approach:\n- Incision from coracoid process towards deltoid tuberosity.\n- Develop interval between Deltoid (axillary nerve) and Pectoralis Major (medial/lateral pectoral nerves).\n- Cephalic vein usually retracted laterally with deltoid.\n- Incise clavipectoral fascia; identify conjoint tendon medially.\n\nDeltoid-Split Approach:\n- Transverse or longitudinal split between anterior and middle deltoid heads.\n- Axillary nerve crosses precisely 5-7cm distal to the lateral acromion edge. Do not split more than 4.5cm without palpating/protecting the nerve with a stay suture.",
            pearls = "Use heavy non-absorbable (#2 or #5 FiberWire) stay sutures through the rotator cuff insertions (subscapularis and infraspinatus) to control displaced tuberosities as joysticks.\nCalcar restoration is the single most critical predictor of preventing post-op varus collapse.",
            warnings = "Axillary nerve injury can occur during distal extension of the deltoid split or excessive medial retraction of the deltoid.",
            orderIndex = 1
        ),
        SectionEntity(
            id = "sec_proximal_humerus_2",
            boneId = "bone_proximal_humerus",
            sectionType = "FIXATION",
            title = "PHILOS / Locking Plate & Calcar Support Screws",
            content = "1. Apply anatomical locking plate 5-8mm distal to the top of the greater tuberosity and 10mm posterior to the bicipital groove.\n2. Place the plate height-adjusting screw or temporary K-wire.\n3. Insert inferomedial calcar screws directed into the dense inferior humeral head quadrant. Calcar screws resist varus displacement forces.\n4. Thread cuff stay sutures through the eyelets of the plate to neutralize rotator cuff deforming vectors.\n5. Check multi-planar fluoroscopy (True AP, Scapular Y, Axillary view) to confirm screw tips are 5mm short of the subchondral plate.",
            pearls = "Never place the plate too high (>8mm above tuberosity) to avoid subacromial impingement on abduction.\nAugment severe osteoporotic bone with intramedullary allograft fibular strut or calcium phosphate cement.",
            warnings = "Intra-articular screw penetration is common if relying solely on standard AP views. Axillary views are mandatory.",
            orderIndex = 2
        ),

        // Femoral Neck / Intertrochanteric Sections
        SectionEntity(
            id = "sec_femoral_neck_1",
            boneId = "bone_femoral_neck",
            sectionType = "REDUCTION",
            title = "Intertrochanteric Reduction Maneuvers & Fluoroscopy",
            content = "1. Position patient on fracture traction table. Secure well leg in lithotomy or scissored position.\n2. Apply longitudinal traction in neutral rotation.\n3. Apply internal rotation (15-20°) to match femoral neck anteversion and restore anterior cortical contact.\n4. Fluoroscopic checks: On AP view, medial cortex of neck should align with medial femoral calcar. On Lateral view, anterior femoral neck cortex must be collinear with anterior shaft.",
            pearls = "Slight valgus reduction (5-10°) is preferred over any varus. Varus malreduction dramatically increases lag screw cutout.\nIf irreducible closed, use a percutaneous anterior bone hook or schanz pin in the proximal fragment to elevate the apex anterior sag.",
            warnings = "Accepting varus reduction or posterior sag is a direct cause of mechanical construct failure.",
            orderIndex = 1
        ),
        SectionEntity(
            id = "sec_femoral_neck_2",
            boneId = "bone_femoral_neck",
            sectionType = "FIXATION",
            title = "Cephalomedullary Nailing & Tip-Apex Distance (TAD) Rule",
            content = "1. Entry Point: Tip of the greater trochanter on AP view; junction of anterior 1/3 and posterior 2/3 on lateral view.\n2. Guide Pin Trajectory: Aim center-center on both AP and lateral views into the femoral head subchondral bone.\n3. Baumgaertner's Tip-Apex Distance (TAD): Sum of the distance from the tip of the lag screw to the apex of the femoral head on AP and Lateral views. TAD MUST BE LESS THAN 25mm.\n4. Distal Locking: Static distal locking for unstable reverse obliquity patterns; dynamic or no distal locking for stable pertrochanteric fractures with good bone contact.",
            pearls = "A TAD < 20mm essentially eliminates the risk of lag screw cutout regardless of patient bone density.\nUse helical blade or dual-integrated compression screws in osteoporotic bone to resist rotational cutout.",
            warnings = "A starting point placed too far medial on the trochanter risks fracturing the lateral trochanteric wall.",
            orderIndex = 2
        ),

        // Femoral Shaft Sections
        SectionEntity(
            id = "sec_femoral_shaft_1",
            boneId = "bone_femoral_shaft",
            sectionType = "FIXATION",
            title = "Antegrade Reamed Intramedullary Nailing",
            content = "1. Trochanteric Entry: Tip of greater trochanter, slightly medial on AP view, inline with medullary canal on lateral view.\n2. Piriformis Entry: Collinear with long axis on AP/lateral, lower hoop stress, but risks damaging the ascending branch of the medial femoral circumflex artery (AVN risk in adolescents).\n3. Reaming Technique: Ream sequentially in 0.5mm increments until positive cortical chatter is felt for 10-15cm. Over-ream by 1.0mm to 1.5mm beyond the intended nail diameter.\n4. Insertion: Advance nail by hand rotation; never hammer aggressively against resistance to avoid cortical blow-out.",
            pearls = "Maintain traction and neutral rotation (compare patellar orientation with contralateral side) before passing the ball-tipped guidewire across the fracture.\nUse finger-tool or unreamed reduction sleeve to manipulate distal fragment into alignment.",
            warnings = "Malrotation is the most common technical error. Clinical evaluation of hip internal/external rotation symmetry before waking the patient is mandatory.",
            orderIndex = 1
        ),

        // Tibial Plateau Sections
        SectionEntity(
            id = "sec_tibial_plateau_1",
            boneId = "bone_tibial_plateau",
            sectionType = "APPROACH",
            title = "Anterolateral & Posteromedial Dual Approaches",
            content = "Anterolateral Approach:\n- Incision curves from lateral femoral condyle over Gerdy's tubercle down 1cm lateral to tibial crest.\n- Submeniscal arthrotomy: Incise coronary ligament beneath the lateral meniscus and place stay sutures to elevate the meniscus, providing direct visualization of the articular surface.\n\nPosteromedial Approach (Lobbe / Lobenhoffer):\n- Incision 1cm posterior to medial border of proximal tibia with knee in 30° flexion.\n- Mobilize pes anserinus anteriorly or retract gently; elevate medial gastrocnemius posteriorly.\n- Exposes posteromedial shear fragment for buttress plate application.",
            pearls = "Perform submeniscal arthrotomy to directly inspect articular joint reduction under direct vision.\nAlways place a subchondral raft of 3.5mm screws right below the elevated articular cartilage before filling the metaphyseal void with allograft or bone substitute.",
            warnings = "Avoid creating a narrow skin bridge between anterolateral and posteromedial incisions; maintain minimum 7cm bridge to prevent wound necrosis.",
            orderIndex = 1
        ),
        SectionEntity(
            id = "sec_tibial_plateau_2",
            boneId = "bone_tibial_plateau",
            sectionType = "FIXATION",
            title = "Subchondral Raft & Anatomical Lateral Locked Plating",
            content = "1. Elevate depressed articular fragments using a bone tamp inserted through a metaphyseal window or cortical fracture split.\n2. Support the elevated cartilage with provisional subchondral 1.6mm K-wires.\n3. Apply a low-profile pre-contoured anatomical lateral tibial plateau locking plate.\n4. Insert multiple parallel 3.5mm subchondral raft screws spanning the medial and lateral columns.\n5. Pack metaphyseal bone void with cancellous allograft or synthetic calcium phosphate cement.",
            pearls = "The posterior-medial fragment is a tension/shear fragment and MUST be buttressed with an anti-glide plate rather than locked laterally.\nVerify complete coronal and sagittal slope restoration on 10° caudal tilted plateau fluoroscopy.",
            warnings = "Failure to address coronal plane alignment (leaving tibia in valgus or varus >3°) causes rapid post-traumatic osteoarthritis.",
            orderIndex = 2
        ),

        // Ankle Malleoli Sections
        SectionEntity(
            id = "sec_ankle_1",
            boneId = "bone_ankle_malleoli",
            sectionType = "ANATOMY",
            title = "Weber Classification & Lauge-Hansen Mechanisms",
            content = "Danis-Weber System:\n- Weber A: Below syndesmosis (adduction mechanism, stable or medial vertical shear).\n- Weber B: At syndesmosis level (spiral, supination-external rotation SER, syndesmosis intact in 50%).\n- Weber C: Above syndesmosis (pronation-external rotation PER or Maisonneuve, syndesmosis disrupted).\n\nLauge-Hansen SER Stages:\nStage 1: Anterior inferior tibiofibular ligament (AITFL) tear.\nStage 2: Spiral fibula fracture (anterior-inferior to posterior-superior).\nStage 3: Posterior inferior tibiofibular ligament (PITFL) or posterior malleolus fracture.\nStage 4: Deltoid ligament rupture or transverse medial malleolus fracture.",
            pearls = "On mortise radiograph, medial clear space must be <= 4mm and equal to superior clear space. >4mm indicates deltoid tear and unstable mortise.\nAlways palpate the proximal fibula up to the fibular head in every ankle injury to rule out a high Maisonneuve fracture.",
            warnings = "Missing a posterior malleolus fragment >20% of articular surface or leaving syndesmotic malreduction leads to rapid joint degeneration.",
            orderIndex = 1
        ),
        SectionEntity(
            id = "sec_ankle_2",
            boneId = "bone_ankle_malleoli",
            sectionType = "FIXATION",
            title = "Lateral Fibula Plating, Medial Screws & Syndesmosis Testing",
            content = "1. Lateral Fibula: Posterolateral antiglide plate (biomechanically superior, lower hardware irritation) vs lateral neutralization plate with 3.5mm lag screw.\n2. Medial Malleolus: Two parallel 4.0mm partially threaded cancellous screws perpendicular to the fracture line.\n3. Syndesmosis Stress Testing: Cotton test (hook pulling fibula laterally under fluoroscopy) or external rotation stress test. If >2mm widening occurs on mortise view, fix syndesmosis.\n4. Syndesmotic Fixation: Suture-button dynamic construct (TightRope) or one/two 3.5mm/4.5mm quadricortical or tricortical screws placed 2-3cm above the joint line angled 25-30° anteriorly.",
            pearls = "Angle syndesmotic screws 25-30° anteriorly to match the anatomical axis of the fibula to the tibia.\nPosterolateral antiglide plating allows bicortical screw purchase even in the distal tip without entering the joint.",
            warnings = "Over-tightening syndesmotic screws in extreme plantarflexion or malreducing fibula in the incisura causes persistent pain and restricted ankle dorsiflexion.",
            orderIndex = 2
        ),

        // Pediatric Supracondylar Sections
        SectionEntity(
            id = "sec_peds_supracondylar_1",
            boneId = "bone_peds_supracondylar",
            sectionType = "REDUCTION",
            title = "Closed Reduction Maneuver & Pinning Configuration",
            content = "1. Milking Maneuver: Apply longitudinal traction with elbow in 30° flexion to disengage brachialis muscle puckering.\n2. Coronal Plane Correction: Correct medial/lateral translation and varus/valgus tilt first.\n3. Hyperflexion & Pronation/Supination: While maintaining traction, apply anterior pressure to olecranon with thumb while flexing elbow >100° with forearm in pronation (for posterolateral displacement) or supination (for posteromedial displacement).\n4. Pinning: Insert two or three 1.6mm or 2.0mm lateral divergent pins engaging both the lateral column and opposite medial cortex.",
            pearls = "Lateral divergent pins provide equal biomechanical stability to crossed pins while eliminating the 4-8% risk of ulnar nerve injury from medial pin placement.\nEnsure minimum 2mm pin spread at the fracture site to prevent rotational loss.",
            warnings = "If radial pulse is absent after reduction, extend elbow to 80-90°. Never leave a pink pulseless hand in extreme hyperflexion.",
            orderIndex = 1
        )
    )

    val images = listOf(
        ImageEntity(
            id = "img_distal_radius_fixation",
            boneId = "bone_distal_radius",
            sectionId = "sec_distal_radius_4",
            title = "Distal Radius Volar Locking Plate & Watershed Zone",
            description = "Anatomical plate positioning proximal to the watershed line with subchondral raft screws supporting the scaphoid and lunate facets.",
            drawableResName = "fig_distal_radius_fixation",
            figureNumber = "Fig 1.1",
            tags = "Volar Plate, Watershed Line, FCR, Radius"
        ),
        ImageEntity(
            id = "img_tibial_plateau_fixation",
            boneId = "bone_tibial_plateau",
            sectionId = "sec_tibial_plateau_2",
            title = "Lateral Tibial Plateau Raft Plate & Subchondral Support",
            description = "Dual column alignment, raft screw array beneath elevated articular cartilage and metaphyseal void packing.",
            drawableResName = "fig_tibial_plateau_fixation",
            figureNumber = "Fig 2.1",
            tags = "Tibial Plateau, Schatzker, Raft Screws, Knee"
        ),
        ImageEntity(
            id = "img_hero_banner",
            boneId = "bone_distal_radius",
            sectionId = null,
            title = "Ortho Tips 3D Anatomical Guide",
            description = "Surgical navigation and biomechanical construct reference.",
            drawableResName = "ortho_hero_banner",
            figureNumber = "Fig 0.1",
            tags = "Hero, Overview, Anatomy"
        )
    )
}
