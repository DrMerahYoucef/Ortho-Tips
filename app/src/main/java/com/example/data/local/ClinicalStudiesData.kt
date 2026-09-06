package com.example.data.local

import com.example.data.local.model.ClassificationGrade
import com.example.data.local.model.ClinicalClassification
import com.example.data.local.model.ClinicalStudy

object ClinicalStudiesData {

    val landmarkStudies: List<ClinicalStudy> = listOf(
        ClinicalStudy(
            id = "sprint_2008",
            shortName = "SPRINT",
            title = "Study to Prospectively Evaluate Reamed IntramedullAry Nails in Patients with Tibial Fractures",
            specialty = "Trauma",
            journalYear = "NEJM 2008",
            evidenceLevel = "Level I RCT",
            patientCount = "1,319 patients",
            clinicalQuestion = "Does reamed vs. unreamed intramedullary nailing improve rates of reoperation and union in tibial shaft fractures?",
            studyDesign = "Multicenter, blinded randomized controlled trial across 29 clinical trauma centers in the US, Canada, and Netherlands.",
            primaryOutcome = "Composite of reoperation to promote fracture union or treat infection within 1 year.",
            keyFindings = "In closed tibial fractures, reamed nailing significantly reduced reoperation rates compared to unreamed nailing (risk ratio 0.67, p = 0.01). In open fractures, reamed nailing showed no statistically significant difference in reoperation (risk ratio 1.12, p = 0.62).",
            operativeImplication = "Reaming is recommended as standard of care for closed tibial shaft fractures treated with intramedullary nails. For open fractures, reamed and unreamed techniques show comparable reoperation rates.",
            clinicalPearls = listOf(
                "Gentle reaming delivers autologous bone graft across the fracture site.",
                "Reaming allows insertion of larger-diameter, mechanically stiffer nail constructs.",
                "Unreamed nails were historically feared to cause less endosteal ischemia, but SPRINT proved reamed nails yield lower reoperation rates in closed fractures."
            )
        ),
        ClinicalStudy(
            id = "flow_2015",
            shortName = "FLOW",
            title = "Fluid Lavage of Open Wounds in Orthopaedic Trauma",
            specialty = "Trauma",
            journalYear = "NEJM 2015",
            evidenceLevel = "Level I RCT",
            patientCount = "2,447 patients",
            clinicalQuestion = "What is the optimal irrigation pressure (high, low, or very low) and irrigation fluid (castile soap vs. normal saline) for open fractures?",
            studyDesign = "2x3 factorial randomized controlled trial conducted across 41 clinical centers in 5 countries.",
            primaryOutcome = "Reoperation within 12 months for wound infection, local wound-healing issues, or nonunion.",
            keyFindings = "Castile soap was associated with a higher rate of reoperation than normal saline (14.8% vs 11.6%, HR 1.32, p = 0.01). Reoperation rates were comparable across all three irrigation pressure groups (high: 11.5%, low: 13.2%, very low: 13.7%).",
            operativeImplication = "Irrigate open fracture wounds with normal saline alone. High-pressure pulsatile lavage is unnecessary and does not reduce infection over low/gravity-flow saline lavage.",
            clinicalPearls = listOf(
                "Normal saline without additives is the gold standard irrigation solution.",
                "Surfactant soaps irritate host tissues and increase reoperation rates.",
                "Gentle low-pressure irrigation clears debris without driving bacteria deeper into cancellous bone channels."
            )
        ),
        ClinicalStudy(
            id = "drafft_2014",
            shortName = "DRAFFT",
            title = "Distal Radius Acute Fracture Fixation Trial",
            specialty = "Upper Extremity",
            journalYear = "BMJ 2014 / Lancet 2015",
            evidenceLevel = "Level I RCT",
            patientCount = "461 patients",
            clinicalQuestion = "Does volar locking plate fixation provide superior patient-reported wrist function over percutaneous Kirschner-wire fixation for dorsally displaced distal radius fractures?",
            studyDesign = "Pragmatic, multicenter randomized controlled superiority trial across 18 UK trauma centers.",
            primaryOutcome = "Patient-Reported Wrist Evaluation (PRWE) score at 12 months.",
            keyFindings = "No clinically important difference in PRWE score at 12 months between volar locking plate (18.0) and K-wire fixation (14.8). Early 6-week functional scores favored plates, but differences completely equalized by 3 to 12 months.",
            operativeImplication = "Volar locking plates cost approximately 4 times more than K-wires without long-term functional superiority for reducible fractures. K-wire fixation remains a cost-effective, clinically sound option.",
            clinicalPearls = listOf(
                "Plate fixation permits earlier wrist mobilization and quicker short-term return to function.",
                "At 1 year, patient-reported outcome scores are virtually identical between plating and pinning.",
                "Complex intra-articular step-offs and volar shearing fragments still strongly mandate anatomical plate fixation."
            )
        ),
        ClinicalStudy(
            id = "aim_2016",
            shortName = "AIM",
            title = "Ankle Injury Management Trial: Fibular Nail vs ORIF in Older Adults",
            specialty = "Foot & Ankle",
            journalYear = "JAMA 2016",
            evidenceLevel = "Level I RCT",
            patientCount = "620 patients",
            clinicalQuestion = "Does closed reduction and casting (close-contact casting) or intramedullary fibular nailing yield equivalent outcomes to open reduction internal fixation (ORIF) in patients over 60 with unstable ankle fractures?",
            studyDesign = "Multicenter pragmatic randomized trial across 24 UK centers in adults aged 60 and older.",
            primaryOutcome = "Olerud-Molander Ankle Score (OMAS) at 6 months.",
            keyFindings = "Close-contact casting and minimally invasive fixation provided equivalent 6-month functional outcomes compared with standard open surgery. Surgical wound complications were significantly higher in traditional ORIF (10% vs 1%).",
            operativeImplication = "In fragile, osteoporotic, or compromised soft-tissue envelopes in elderly patients, close-contact casting or percutaneous/intramedullary fibular nailing avoids major wound dehiscence and infection while providing comparable mobility.",
            clinicalPearls = listOf(
                "Elderly ankle skin is vulnerable to full-thickness necrosis after extensile lateral incisions.",
                "Close contact casting requires meticulous casting technique under image intensifier guidance.",
                "Fibular intramedullary nails provide excellent load-sharing stability with minimal soft-tissue dissection."
            )
        ),
        ClinicalStudy(
            id = "crash2_2010",
            shortName = "CRASH-2",
            title = "Effects of Tranexamic Acid on Death and Vascular Occlusive Events in Trauma",
            specialty = "Trauma",
            journalYear = "Lancet 2010",
            evidenceLevel = "Level I RCT",
            patientCount = "20,211 patients",
            clinicalQuestion = "Does early administration of tranexamic acid (TXA) reduce mortality in bleeding trauma patients, and is there an increased risk of thromboembolism?",
            studyDesign = "Large-scale, international randomized placebo-controlled trial across 274 hospitals in 40 countries.",
            primaryOutcome = "All-cause mortality at 28 days.",
            keyFindings = "TXA significantly reduced risk of death due to bleeding (4.9% vs 5.7%, RR 0.85, p = 0.0077) without increasing vascular occlusive events (DVT/PE). Benefit was greatest when given within 1 to 3 hours of injury.",
            operativeImplication = "Administer TXA (1g IV loading dose over 10 min followed by 1g over 8 hours) early in major orthopedic trauma and pelvic/acetabular fractures with significant hemorrhage. TXA is also proven safe in elective arthroplasty.",
            clinicalPearls = listOf(
                "Administration after 3 hours of injury showed no benefit and potentially increased mortality.",
                "TXA acts by inhibiting fibrinolysis through reversible binding to plasminogen.",
                "Routine perioperative IV/topical TXA in total joint arthroplasty halves blood transfusion rates."
            )
        ),
        ClinicalStudy(
            id = "hipattack_2020",
            shortName = "HIP ATTACK",
            title = "Accelerated Surgery versus Standard Care in Patients with Hip Fracture",
            specialty = "Arthroplasty",
            journalYear = "Lancet 2020",
            evidenceLevel = "Level I RCT",
            patientCount = "2,970 patients",
            clinicalQuestion = "Does accelerated surgery (operating within 6 hours of diagnosis) reduce mortality and major complications in elderly hip fracture patients?",
            studyDesign = "International multicenter RCT across 17 centers in 10 countries.",
            primaryOutcome = "Mortality and major complications at 90 days.",
            keyFindings = "Overall 90-day mortality was similar between accelerated surgery (median 6 hrs) and standard care (median 24 hrs) (9% vs 10%, HR 0.91). However, accelerated surgery significantly reduced delirium (9% vs 12%, p = 0.018), urinary tract infections, and shortened hospital stay.",
            operativeImplication = "Expedited hip fracture surgery (under 24-36 hours) is essential to minimize delirium, pressure sores, and pain, though operating ultra-emergently under 6 hours does not confer mortality reduction over prompt daytime scheduling.",
            clinicalPearls = listOf(
                "Standard target guideline remains surgery within 24 to 36 hours of presentation.",
                "Accelerated surgery markedly alleviates preoperative pain and opioid requirements.",
                "Optimizing medical comorbidities must not cause prolonged delays beyond 36 hours."
            )
        ),
        ClinicalStudy(
            id = "health_2019",
            shortName = "HEALTH",
            title = "Total Hip Arthroplasty versus Hemiarthroplasty for Hip Fracture in Elderly Patients",
            specialty = "Arthroplasty",
            journalYear = "NEJM 2019",
            evidenceLevel = "Level I RCT",
            patientCount = "1,495 patients",
            clinicalQuestion = "Does total hip arthroplasty (THA) or hemiarthroplasty lead to fewer secondary hip procedures in independent older adults with displaced femoral neck fractures?",
            studyDesign = "Multicenter international RCT across 80 centers in 10 countries involving patients 50+ years who were ambulatory prior to injury.",
            primaryOutcome = "Unplanned secondary hip procedure over 24 months.",
            keyFindings = "The rate of secondary hip procedures over 24 months did not differ significantly between THA and hemiarthroplasty (7.9% vs 8.3%, HR 0.95, p = 0.79). THA provided clinically modest improvements in function and quality of life, but with higher dislocation risk.",
            operativeImplication = "Hemiarthroplasty is a robust, reliable operation with low complication rates for elderly patients. THA is reserved for active, high-demand community ambulators or patients with pre-existing symptomatic hip osteoarthritis.",
            clinicalPearls = listOf(
                "THA had a higher risk of postoperative dislocation compared to hemiarthroplasty.",
                "Pre-existing hip osteoarthritis strongly favors THA over hemiarthroplasty.",
                "Dual mobility or larger femoral heads reduce dislocation when THA is selected for fracture."
            )
        ),
        ClinicalStudy(
            id = "faith_2017",
            shortName = "FAITH",
            title = "Fixation using Alternative Implants for the Treatment of Hip Fractures",
            specialty = "Trauma",
            journalYear = "Lancet 2017",
            evidenceLevel = "Level I RCT",
            patientCount = "1,079 patients",
            clinicalQuestion = "Does a sliding hip screw (SHS) or multiple cancellous screws (MCS) reduce revision surgery in femoral neck fractures?",
            studyDesign = "Multicenter RCT across 81 clinical centers in 8 countries in adults aged 50+ years.",
            primaryOutcome = "Reoperation within 24 months to promote healing or manage implant complications.",
            keyFindings = "Reoperation rates were comparable overall (SHS 20% vs MCS 22%, HR 0.96). However, in smokers, basicervical fractures, and displaced fractures, SHS demonstrated superior mechanical stability and lower reoperation.",
            operativeImplication = "Sliding hip screw is favored for basicervical patterns and fractures prone to collapse/shortening. Cancellous screws are favored for nondisplaced subcapital patterns.",
            clinicalPearls = listOf(
                "Basicervical fractures are biomechanically extracapsular and require fixed-angle devices like SHS.",
                "Femoral neck shortening occurs frequently with multiple parallel screws.",
                "Pauwels angle > 50 degrees creates high shear stresses that overwhelm cancellous screws alone."
            )
        ),
        ClinicalStudy(
            id = "crossfire_2020",
            shortName = "CROSSFIRE",
            title = "Clavicle Operative versus Nonoperative Surgery Follow-up in Radiographic Evaluation",
            specialty = "Upper Extremity",
            journalYear = "J Bone Joint Surg Am 2020",
            evidenceLevel = "Level I RCT",
            patientCount = "300 patients",
            clinicalQuestion = "Does plate fixation vs nonoperative treatment with sling improve 1-year QuickDASH score for displaced midshaft clavicle fractures?",
            studyDesign = "Multicenter prospective randomized controlled trial in active adult patients aged 18 to 65 years.",
            primaryOutcome = "QuickDASH score and patient satisfaction at 12 months.",
            keyFindings = "Plate fixation achieved lower nonunion rates (1.4% vs 14.6%) and faster initial recovery at 6 weeks. At 1 year, QuickDASH scores were statistically similar (Plate: 6.2 vs Sling: 9.8). Plate removal surgery was required in 18% of operative patients.",
            operativeImplication = "Nonoperative treatment remains safe and effective for the majority of midshaft clavicle fractures. Operative plating is indicated for active athletes, 100% displacement, shortening > 2cm, or neurovascular compromise.",
            clinicalPearls = listOf(
                "Nonoperative treatment has a ~15% nonunion risk in severely displaced fractures.",
                "Hardware irritation over the subcutaneous clavicle necessitates secondary plate removal in ~15-20%.",
                "Superior plating offers tensile biomechanical strength, while anteroinferior plating minimizes hardware prominence."
            )
        )
    )

    val clinicalClassifications: List<ClinicalClassification> = listOf(
        ClinicalClassification(
            id = "gustilo_anderson",
            name = "Gustilo-Anderson Classification",
            anatomicRegion = "Open Fractures",
            description = "The universal gold standard classification for open fractures, prognostic of infection rate, time to union, and limb salvage requirements.",
            grades = listOf(
                ClassificationGrade(
                    grade = "Type I",
                    criteria = "Clean skin wound < 1 cm, usually inside-out puncture; minimal soft-tissue contusion; simple fracture pattern.",
                    managementTakeaway = "1st-gen Cephalosporin (Cefazolin). Urgent I&D within 24 hours. Primary or delayed closure."
                ),
                ClassificationGrade(
                    grade = "Type II",
                    criteria = "Laceration > 1 cm without extensive soft-tissue damage, flaps, or avulsions; mild/moderate crush component.",
                    managementTakeaway = "1st-gen Cephalosporin. Formal operative debridement and skeletal stabilization."
                ),
                ClassificationGrade(
                    grade = "Type IIIA",
                    criteria = "Extensive soft-tissue damage or high-energy trauma (shotgun, farm, MVA) with adequate periosteal coverage of bone.",
                    managementTakeaway = "Cefazolin + Aminoglycoside (Gentamicin). Add Penicillin/Ampicillin if barnyard/soil contamination."
                ),
                ClassificationGrade(
                    grade = "Type IIIB",
                    criteria = "Extensive soft-tissue loss with periosteal stripping and bone exposure. Requires local flap or free vascularized flap coverage.",
                    managementTakeaway = "Broad-spectrum IV antibiotics. Serial debridements. Plastic surgery consult for flap coverage within 72 hrs."
                ),
                ClassificationGrade(
                    grade = "Type IIIC",
                    criteria = "Any open fracture associated with arterial injury requiring vascular repair to salvage limb viability.",
                    managementTakeaway = "Immediate surgical emergency! Shunt first, stabilize bone with spanning ex-fix, definitive vascular repair."
                )
            ),
            highYieldPearl = "Classify wounds intraoperatively AFTER formal surgical debridement, not in the emergency department, as true extent of contamination and periosteal stripping becomes evident only in the OR."
        ),
        ClinicalClassification(
            id = "schatzker",
            name = "Schatzker Classification",
            anatomicRegion = "Tibial Plateau",
            description = "Guides operative approach and fixation strategy for tibial plateau fractures based on mechanism and plateau involvement.",
            grades = listOf(
                ClassificationGrade(
                    grade = "Type I",
                    criteria = "Lateral split fracture without articular depression. Typical in younger patients with dense cancellous bone.",
                    managementTakeaway = "Percutaneous cannulated screws with washers or lateral buttress plate."
                ),
                ClassificationGrade(
                    grade = "Type II",
                    criteria = "Lateral split-depression fracture. Most common tibial plateau fracture pattern.",
                    managementTakeaway = "Lateral submeniscal arthrotomy/fluoroscopy, elevate depressed joint surface, subchondral bone graft/void filler, lateral buttress locking plate."
                ),
                ClassificationGrade(
                    grade = "Type III",
                    criteria = "Pure central lateral plateau depression without cortical split. Typical in osteoporotic bone.",
                    managementTakeaway = "Cortical window in lateral metaphyseal wall, elevate articular joint with tamp, bone graft, raft screws/plate."
                ),
                ClassificationGrade(
                    grade = "Type IV",
                    criteria = "Medial tibial plateau fracture (split or depression). High energy, associated with knee dislocation and peroneal/popliteal injury.",
                    managementTakeaway = "Check vascular exam! Medial or posteromedial approach with stout medial buttress plate."
                ),
                ClassificationGrade(
                    grade = "Type V",
                    criteria = "Bicondylar tibial plateau fracture with continuous tibial shaft.",
                    managementTakeaway = "Spanning knee external fixator until soft tissues wrinkle. Dual incisional plating (anterolateral + posteromedial)."
                ),
                ClassificationGrade(
                    grade = "Type VI",
                    criteria = "Metaphyseal-diaphyseal dissociation with complete separation of plateau articular block from shaft.",
                    managementTakeaway = "Temporary knee ex-fix. High compartment syndrome risk! Dual approach locking plates or hybrid frame."
                )
            ),
            highYieldPearl = "Schatzker IV, V, and VI are high-energy injuries associated with compartment syndrome (up to 30%) and common peroneal or popliteal vessel trauma. Staged treatment with temporary bridging external fixation is standard."
        ),
        ClinicalClassification(
            id = "garden",
            name = "Garden Classification",
            anatomicRegion = "Femoral Neck",
            description = "Subcapital femoral neck fracture classification based on AP radiograph of hip and alignment of trabecular lines.",
            grades = listOf(
                ClassificationGrade(
                    grade = "Stage I",
                    criteria = "Incomplete, impacted in valgus, trabeculae angulated laterally. Stable.",
                    managementTakeaway = "In-situ percutaneous pinning with 3 cannulated parallel inverted-triangle screws."
                ),
                ClassificationGrade(
                    grade = "Stage II",
                    criteria = "Complete fracture across neck without displacement. Trabeculae interrupted but not angulated.",
                    managementTakeaway = "Internal fixation with 3 parallel cancellous screws or sliding hip screw."
                ),
                ClassificationGrade(
                    grade = "Stage III",
                    criteria = "Complete fracture with partial displacement. Femoral head tilted into varus; trabeculae do not align with acetabulum.",
                    managementTakeaway = "Young (<60): Urgent anatomical reduction + rigid fixation. Elderly: Hemiarthroplasty or THA."
                ),
                ClassificationGrade(
                    grade = "Stage IV",
                    criteria = "Complete fracture with total displacement. Femoral head fragments are completely disassociated and trabeculae align normally with acetabulum.",
                    managementTakeaway = "Young: Open/closed anatomic reduction + fixation. Elderly (>65-70): Arthroplasty (Hemi vs THA)."
                )
            ),
            highYieldPearl = "Displaced femoral neck fractures (Garden III & IV) in elderly patients carry a 30-40% avascular necrosis (AVN) rate and 20-30% nonunion rate if fixed; arthroplasty provides immediate full weight-bearing and eliminates AVN risk."
        ),
        ClinicalClassification(
            id = "neer",
            name = "Neer Classification",
            anatomicRegion = "Proximal Humerus",
            description = "Based on 4 anatomic segments (Head, Greater Tuberosity, Lesser Tuberosity, Shaft). A segment counts as a 'part' only if displaced > 1 cm or angulated > 45 degrees.",
            grades = listOf(
                ClassificationGrade(
                    grade = "One-Part",
                    criteria = "No fragments displaced > 1cm or angulated > 45 degrees regardless of the number of fracture lines.",
                    managementTakeaway = "Conservative management: Sling immobilization for 2-3 weeks followed by early pendulum/ROM exercises."
                ),
                ClassificationGrade(
                    grade = "Two-Part",
                    criteria = "One segment is displaced relative to the remaining three intact/connected segments.",
                    managementTakeaway = "Greater tuberosity displacement > 5mm requires fixation. Surgical neck: locked plate or IM nail."
                ),
                ClassificationGrade(
                    grade = "Three-Part",
                    criteria = "Two displaced segments (e.g. surgical neck + displaced greater tuberosity). Unattached head still articulates with glenoid.",
                    managementTakeaway = "Anatomic locked plating (PHILOS type) with calcar screws and tuberosity suture cerclage."
                ),
                ClassificationGrade(
                    grade = "Four-Part",
                    criteria = "All four segments displaced. High risk of articular vascular compromise and AVN of humeral head (> 70%).",
                    managementTakeaway = "Young: Attempt ORIF with vascularized bone graft/fibula strut. Elderly: Reverse Total Shoulder Arthroplasty (rTSA)."
                )
            ),
            highYieldPearl = "Greater tuberosity displacement as small as 5mm in active overhead athletes causes mechanical subacromial impingement and mandates surgical reduction and fixation."
        )
    )
}
