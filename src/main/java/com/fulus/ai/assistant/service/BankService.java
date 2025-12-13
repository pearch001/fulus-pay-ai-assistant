package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.BankListResponse;
import com.fulus.ai.assistant.dto.NameEnquiryRequest;
import com.fulus.ai.assistant.dto.NameEnquiryResponse;
import com.fulus.ai.assistant.entity.BankInfo;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.repository.BankInfoRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for bank operations (bank list, name enquiry)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankService {

    private final BankInfoRepository bankInfoRepository;
    private final UserRepository userRepository;

    // Internal bank code for Fulus Pay
    private static final String INTERNAL_BANK_CODE = "999999";

    // Pool of Nigerian names for simulation
    private static final List<String> NIGERIAN_NAMES = List.of(
            "ADEWALE OLUWASEUN JOHNSON",
            "CHINWE NGOZI OKAFOR",
            "IBRAHIM MUSA ABDULLAHI",
            "FUNMILAYO ADEOLA WILLIAMS",
            "CHUKWUEMEKA IKENNA NWANKWO",
            "AISHA ZAINAB MOHAMMED",
            "OLUWAFEMI BABATUNDE ADEYEMI",
            "NGOZI CHIOMA EZE",
            "YUSUF AHMAD BELLO",
            "BLESSING IFEOMA OKORO",
            "TUNDE AYODEJI AJAYI",
            "AMINA FATIMA HASSAN",
            "EMEKA CHINEDU OKONKWO",
            "FOLAKE TITILAYO OGUNLEYE",
            "USMAN ABDULRAHMAN UMAR"
    );

    private static final Random random = new Random();

    /**
     * Initialize bank list on startup
     */
    @PostConstruct
    @Transactional
    public void initializeBanks() {
        if (bankInfoRepository.count() > 0) {
            log.info("Banks already initialized");
            return;
        }

        log.info("Initializing Nigerian banks...");

        List<BankInfo> banks = List.of(
                BankInfo.builder().bankCode("090267").bankName("Kuda.").active(true).build(),
                BankInfo.builder().bankCode("110072").bankName("78 FINANCE COMPANY LIMITED").active(true).build(),
                BankInfo.builder().bankCode("120001").bankName("9 Payment Service Bank (9PSB)").active(true).build(),
                BankInfo.builder().bankCode("90629").bankName("9jaPay MFB").active(true).build(),
                BankInfo.builder().bankCode("70010").bankName("ABBEY MORTGAGE BANK").active(true).build(),
                BankInfo.builder().bankCode("90260").bankName("Above Only Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("90640").bankName("ABSU MFB").active(true).build(),
                BankInfo.builder().bankCode("90197").bankName("ABU Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("000014").bankName("Access Bank").active(true).build(),
                BankInfo.builder().bankCode("000005").bankName("Access Bank PLC (Diamond)").active(true).build(),
                BankInfo.builder().bankCode("100013").bankName("AccessMobile").active(true).build(),
                BankInfo.builder().bankCode("090134").bankName("ACCION MFB").active(true).build(),
                BankInfo.builder().bankCode("090483").bankName("ADA MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90160").bankName("Addosser MFBB").active(true).build(),
                BankInfo.builder().bankCode("090614").bankName("AELLA MFB").active(true).build(),
                BankInfo.builder().bankCode("090531").bankName("AKU MFB").active(true).build(),
                BankInfo.builder().bankCode("90133").bankName("AL-BARKAH MFB").active(true).build(),
                BankInfo.builder().bankCode("90259").bankName("Alekun Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("090297").bankName("Alert Microfinance Ban").active(true).build(),
                BankInfo.builder().bankCode("90131").bankName("ALLWORKERS MFB").active(true).build(),
                BankInfo.builder().bankCode("90169").bankName("Alphakapital MFB").active(true).build(),
                BankInfo.builder().bankCode("37").bankName("ALTERNATIVE BANK LIMITED").active(true).build(),
                BankInfo.builder().bankCode("90394").bankName("AMAC MFB").active(true).build(),
                BankInfo.builder().bankCode("90180").bankName("Amju MFB").active(true).build(),
                BankInfo.builder().bankCode("90116").bankName("AMML MFB").active(true).build(),
                BankInfo.builder().bankCode("90645").bankName("Amucha MFB").active(true).build(),
                BankInfo.builder().bankCode("90143").bankName("APEKS Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90001").bankName("ASOSavings").active(true).build(),
                BankInfo.builder().bankCode("090287").bankName("Asset Matrix MFB").active(true).build(),
                BankInfo.builder().bankCode("090473").bankName("ASSETS MFB").active(true).build(),
                BankInfo.builder().bankCode("90172").bankName("Astrapolis MFB").active(true).build(),
                BankInfo.builder().bankCode("90264").bankName("Auchi Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("090478").bankName("Avuenegbe MFB").active(true).build(),
                BankInfo.builder().bankCode("90633").bankName("Awacash MFB").active(true).build(),
                BankInfo.builder().bankCode("90662").bankName("Awesome MFB").active(true).build(),
                BankInfo.builder().bankCode("90540").bankName("Aztec MFB").active(true).build(),
                BankInfo.builder().bankCode("090188").bankName("Baines Credit MFB").active(true).build(),
                BankInfo.builder().bankCode("090651").bankName("BAM MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("090529").bankName("Bankly MFB").active(true).build(),
                BankInfo.builder().bankCode("90127").bankName("BC Kash MFB").active(true).build(),
                BankInfo.builder().bankCode("090672").bankName("BELLBANK MFB").active(true).build(),
                BankInfo.builder().bankCode("090413").bankName("Benysta Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90618").bankName("Berachah MFB").active(true).build(),
                BankInfo.builder().bankCode("090615").bankName("BestStar MFB").active(true).build(),
                BankInfo.builder().bankCode("090683").bankName("BETHEL MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("090117").bankName("Boctrust Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90501").bankName("BOROMU MFB").active(true).build(),
                BankInfo.builder().bankCode("90454").bankName("Borstal MFB").active(true).build(),
                BankInfo.builder().bankCode("90176").bankName("Bosak MFB").active(true).build(),
                BankInfo.builder().bankCode("090148").bankName("Bowen MFB").active(true).build(),
                BankInfo.builder().bankCode("050006").bankName("Branch International Financial Services").active(true).build(),
                BankInfo.builder().bankCode("70015").bankName("Brent Mortgage Bank").active(true).build(),
                BankInfo.builder().bankCode("90613").bankName("BUILD MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90661").bankName("Bundi MFB").active(true).build(),
                BankInfo.builder().bankCode("90655").bankName("Bunkure MFB").active(true).build(),
                BankInfo.builder().bankCode("090682").bankName("BUYPOWER MICROFINANACE BANK").active(true).build(),
                BankInfo.builder().bankCode("90647").bankName("Canaan MFB").active(true).build(),
                BankInfo.builder().bankCode("90445").bankName("Capstone MFB").active(true).build(),
                BankInfo.builder().bankCode("100026").bankName("Carbon").active(true).build(),
                BankInfo.builder().bankCode("090360").bankName("CashConnect MFB").active(true).build(),
                BankInfo.builder().bankCode("90649").bankName("Cashrite MFB").active(true).build(),
                BankInfo.builder().bankCode("90154").bankName("CEMCS MFB").active(true).build(),
                BankInfo.builder().bankCode("090397").bankName("Chanelle MFB").active(true).build(),
                BankInfo.builder().bankCode("90470").bankName("Changan RTS Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90141").bankName("Chikum Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90144").bankName("CIT Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("000009").bankName("Citi Bank").active(true).build(),
                BankInfo.builder().bankCode("70027").bankName("Citycode Mortgage Bank ").active(true).build(),
                BankInfo.builder().bankCode("090553").bankName("Consistent Trust MFB (CTMFB)").active(true).build(),
                BankInfo.builder().bankCode("090130").bankName("CONSUMER MFB").active(true).build(),
                BankInfo.builder().bankCode("100032").bankName("Contec Global").active(true).build(),
                BankInfo.builder().bankCode("90365").bankName("CoreStep MicroFinance Bank").active(true).build(),
                BankInfo.builder().bankCode("060001").bankName("Coronation").active(true).build(),
                BankInfo.builder().bankCode("50001").bankName("County Finance").active(true).build(),
                BankInfo.builder().bankCode("70006").bankName("Covenant MFB").active(true).build(),
                BankInfo.builder().bankCode("90159").bankName("Credit Afrique MFB").active(true).build(),
                BankInfo.builder().bankCode("110049").bankName("Credit Direct Limited").active(true).build(),
                BankInfo.builder().bankCode("90611").bankName("Creditville MFB").active(true).build(),
                BankInfo.builder().bankCode("090560").bankName("CRUST MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90673").bankName("DAVENPORT MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90167").bankName("Daylight Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("70023").bankName("Delta Trust Mortgage Bank").active(true).build(),
                BankInfo.builder().bankCode("90404").bankName("DOJE Microfinance Bank Limited").active(true).build(),
                BankInfo.builder().bankCode("090156").bankName("e-BARCs MFB").active(true).build(),
                BankInfo.builder().bankCode("90674").bankName("EARNWELL MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("100021").bankName("Eartholeum").active(true).build(),
                BankInfo.builder().bankCode("000010").bankName("Ecobank Bank").active(true).build(),
                BankInfo.builder().bankCode("100008").bankName("Ecobank Xpress Account").active(true).build(),
                BankInfo.builder().bankCode("090694").bankName("EJINDU MFB").active(true).build(),
                BankInfo.builder().bankCode("090097").bankName("Ekondo MFB").active(true).build(),
                BankInfo.builder().bankCode("90114").bankName("EmpireTrust Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("90656").bankName("Entity MFB").active(true).build(),
                BankInfo.builder().bankCode("90189").bankName("Esan MFB").active(true).build(),
                BankInfo.builder().bankCode("90166").bankName("Eso-E Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("100006").bankName("eTranzact").active(true).build(),
                BankInfo.builder().bankCode("90678").bankName("Excel Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("090541").bankName("Excellent MFB").active(true).build(),
                BankInfo.builder().bankCode("090551").bankName("FairMoney").active(true).build(),
                BankInfo.builder().bankCode("050009").bankName("Fast Credit Limited").active(true).build(),
                BankInfo.builder().bankCode("90179").bankName("FAST MFB").active(true).build(),
                BankInfo.builder().bankCode("90107").bankName("FBN Morgages Limited").active(true).build(),
                BankInfo.builder().bankCode("60002").bankName("FBNQuest MERCHANT BANK").active(true).build(),
                BankInfo.builder().bankCode("000003").bankName("FCMB").active(true).build(),
                BankInfo.builder().bankCode("100031").bankName("FCMB Easy Account").active(true).build(),
                BankInfo.builder().bankCode("090409").bankName("FCMB MFB").active(true).build(),
                BankInfo.builder().bankCode("90482").bankName("FEDETH MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("100001").bankName("FET").active(true).build(),
                BankInfo.builder().bankCode("90153").bankName("FFS Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("000007").bankName("Fidelity Bank").active(true).build(),
                BankInfo.builder().bankCode("100019").bankName("Fidelity Mobile").active(true).build(),
                BankInfo.builder().bankCode("90126").bankName("FidFund MFB").active(true).build(),
                BankInfo.builder().bankCode("90111").bankName("FinaTrust Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("090366").bankName("Firmus MFB").active(true).build(),
                BankInfo.builder().bankCode("000016").bankName("First Bank of Nigeria").active(true).build(),
                BankInfo.builder().bankCode("70014").bankName("First Generation Mortgage Bank").active(true).build(),
                BankInfo.builder().bankCode("90163").bankName("First Multiple MFB").active(true).build(),
                BankInfo.builder().bankCode("90164").bankName("First Royal Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("100014").bankName("Firstmonie Wallet").active(true).build(),
                BankInfo.builder().bankCode("090107").bankName("FIRSTTRUST MORTGAGE BANK NIG PLC").active(true).build(),
                BankInfo.builder().bankCode("90521").bankName("Foresight Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("70002").bankName("Fortis Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("100016").bankName("FortisMobile").active(true).build(),
                BankInfo.builder().bankCode("90145").bankName("Full range MFB").active(true).build(),
                BankInfo.builder().bankCode("90158").bankName("FUTO MFB").active(true).build(),
                BankInfo.builder().bankCode("90591").bankName("Gabsyn MFB").active(true).build(),
                BankInfo.builder().bankCode("90168").bankName("Gashua Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("70009").bankName("GATEWAY MORTGAGE BANK").active(true).build(),
                BankInfo.builder().bankCode("90586").bankName("Gombe MFB").active(true).build(),
                BankInfo.builder().bankCode("100022").bankName("GoMoney").active(true).build(),
                BankInfo.builder().bankCode("90664").bankName("Good Shepherd Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("090495").bankName("Goodnews MFB").active(true).build(),
                BankInfo.builder().bankCode("090687").bankName("GOSIFECHUKWU MFB").active(true).build(),
                BankInfo.builder().bankCode("90122").bankName("GOWANS MFB").active(true).build(),
                BankInfo.builder().bankCode("90335").bankName("GRANTS MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90599").bankName("Greenacres MFB").active(true).build(),
                BankInfo.builder().bankCode("90178").bankName("GreenBank MFB").active(true).build(),
                BankInfo.builder().bankCode("90195").bankName("Grooming Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("000013").bankName("GTBank Plc").active(true).build(),
                BankInfo.builder().bankCode("090385").bankName("GTI MFB").active(true).build(),
                BankInfo.builder().bankCode("100009").bankName("GTMobile").active(true).build(),
                BankInfo.builder().bankCode("90500").bankName("Gwong MFB").active(true).build(),
                BankInfo.builder().bankCode("90147").bankName("Hackman Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("70017").bankName("Haggai Mortgage Bank").active(true).build(),
                BankInfo.builder().bankCode("090539").bankName("HALO MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90121").bankName("HASAL MFB").active(true).build(),
                BankInfo.builder().bankCode("100017").bankName("Hedonmark").active(true).build(),
                BankInfo.builder().bankCode("000020").bankName("Heritage").active(true).build(),
                BankInfo.builder().bankCode("090175").bankName("HighStreet MFB").active(true).build(),
                BankInfo.builder().bankCode("120002").bankName("Hope PSB").active(true).build(),
                BankInfo.builder().bankCode("90118").bankName("IBILE Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90279").bankName("Ikire MFB").active(true).build(),
                BankInfo.builder().bankCode("090681").bankName("IKOYI ILE MICROFINANCEBANK").active(true).build(),
                BankInfo.builder().bankCode("90430").bankName("Ilora MFB").active(true).build(),
                BankInfo.builder().bankCode("100024").bankName("Imperial Homes Mortgage Bank").active(true).build(),
                BankInfo.builder().bankCode("90670").bankName("IMSU Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90157").bankName("Infinity MFB").active(true).build(),
                BankInfo.builder().bankCode("70016").bankName("Infinity trust Mortgage Bank").active(true).build(),
                BankInfo.builder().bankCode("90434").bankName("Insight MFB").active(true).build(),
                BankInfo.builder().bankCode("90149").bankName("IRL Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("090149").bankName("IRL MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90353").bankName("ISUOFIA MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("000006").bankName("JAIZ Bank").active(true).build(),
                BankInfo.builder().bankCode("090003").bankName("JubileeLife").active(true).build(),
                BankInfo.builder().bankCode("090320").bankName("KADPOLY MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90669").bankName("Kadupe MFB").active(true).build(),
                BankInfo.builder().bankCode("090667").bankName("KAYI MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90554").bankName("Kayvee MFB").active(true).build(),
                BankInfo.builder().bankCode("090554").bankName("KAYVEE MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("090549").bankName("KC MFB").active(true).build(),
                BankInfo.builder().bankCode("090191").bankName("KCMB MFB").active(true).build(),
                BankInfo.builder().bankCode("100015").bankName("Kegow").active(true).build(),
                BankInfo.builder().bankCode("100036").bankName("KEGOW(CHAMSMOBILE)").active(true).build(),
                BankInfo.builder().bankCode("000002").bankName("Keystone Bank").active(true).build(),
                BankInfo.builder().bankCode("90480").bankName("Kolomoni MFB").active(true).build(),
                BankInfo.builder().bankCode("90380").bankName("Kredimoney Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90155").bankName("La Fayette Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("090155").bankName("La Fayette Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90177").bankName("Lapo MFB").active(true).build(),
                BankInfo.builder().bankCode("70012").bankName("LBIC Mortgage Bank").active(true).build(),
                BankInfo.builder().bankCode("90650").bankName("Leadcity MFB").active(true).build(),
                BankInfo.builder().bankCode("090435").bankName("Links MFB").active(true).build(),
                BankInfo.builder().bankCode("7007").bankName("Living Trust Morgage Bank Plc").active(true).build(),
                BankInfo.builder().bankCode("90620").bankName("LOMA MFB").active(true).build(),
                BankInfo.builder().bankCode("29").bankName("LOTUS BANK").active(true).build(),
                BankInfo.builder().bankCode("90265").bankName("Lovonus Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("090685").bankName("M&M MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("100035").bankName("M36").active(true).build(),
                BankInfo.builder().bankCode("090171").bankName("Mainstreet MFB").active(true).build(),
                BankInfo.builder().bankCode("90174").bankName("Malachy MFB").active(true).build(),
                BankInfo.builder().bankCode("90383").bankName("Manny Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("090589").bankName("Mercury MFB").active(true).build(),
                BankInfo.builder().bankCode("90659").bankName("Michael Okpara Uniagric MFB").active(true).build(),
                BankInfo.builder().bankCode("90587").bankName("Microbiz MFB").active(true).build(),
                BankInfo.builder().bankCode("90136").bankName("Microcred Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90281").bankName("Mint-Finex MFB").active(true).build(),
                BankInfo.builder().bankCode("090455").bankName("Mkobo Mfb").active(true).build(),
                BankInfo.builder().bankCode("100011").bankName("Mkudi").active(true).build(),
                BankInfo.builder().bankCode("120003").bankName("Momo PSB").active(true).build(),
                BankInfo.builder().bankCode("100020").bankName("MoneyBox").active(true).build(),
                BankInfo.builder().bankCode("090144").bankName("MONEYFIELD MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("120005").bankName("MoneyMaster PSB").active(true).build(),
                BankInfo.builder().bankCode("90129").bankName("MONEYTRUST MFB").active(true).build(),
                BankInfo.builder().bankCode("090405").bankName("Moniepoint MFB").active(true).build(),
                BankInfo.builder().bankCode("100028").bankName("Mortage Bank").active(true).build(),
                BankInfo.builder().bankCode("070028").bankName("MUTUAL ALLIANCE MORTGAGE BANK").active(true).build(),
                BankInfo.builder().bankCode("90190").bankName("Mutual Benefits MFB").active(true).build(),
                BankInfo.builder().bankCode("90151").bankName("Mutual Trust Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90152").bankName("Nargata MFB").active(true).build(),
                BankInfo.builder().bankCode("90679").bankName("NDDC Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90128").bankName("Ndiorah MFB").active(true).build(),
                BankInfo.builder().bankCode("090675").bankName("NET MFB").active(true).build(),
                BankInfo.builder().bankCode("090205").bankName("New Dawn MFB").active(true).build(),
                BankInfo.builder().bankCode("90108").bankName("New Prudential Bank").active(true).build(),
                BankInfo.builder().bankCode("30001").bankName("Nexim Bank").active(true).build(),
                BankInfo.builder().bankCode("90505").bankName("Nigerian Prisons MFB").active(true).build(),
                BankInfo.builder().bankCode("90194").bankName("NIRSAL National microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("60003").bankName("NOVA MB").active(true).build(),
                BankInfo.builder().bankCode("70001").bankName("NPF MicroFinance Bank").active(true).build(),
                BankInfo.builder().bankCode("090491").bankName("NSUK MFB").active(true).build(),
                BankInfo.builder().bankCode("090676").bankName("NUGGETS MFB").active(true).build(),
                BankInfo.builder().bankCode("90399").bankName("Nwannegadi MFB").active(true).build(),
                BankInfo.builder().bankCode("90333").bankName("Oche MFB").active(true).build(),
                BankInfo.builder().bankCode("90576").bankName("Octopus MFB").active(true).build(),
                BankInfo.builder().bankCode("90654").bankName("Odoakpu MFB").active(true).build(),
                BankInfo.builder().bankCode("90119").bankName("OHAFIA MFB").active(true).build(),
                BankInfo.builder().bankCode("090567").bankName("OK MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90646").bankName("Okengwe MFB").active(true).build(),
                BankInfo.builder().bankCode("90161").bankName("Okpoga MFB").active(true).build(),
                BankInfo.builder().bankCode("090696").bankName("Olive MFB").active(true).build(),
                BankInfo.builder().bankCode("70007").bankName("Omoluabi Mortgage Bank Plc").active(true).build(),
                BankInfo.builder().bankCode("000036").bankName("Optimus Bank").active(true).build(),
                BankInfo.builder().bankCode("90492").bankName("Oraukwu MFB").active(true).build(),
                BankInfo.builder().bankCode("90396").bankName("OSCOTECH MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90542").bankName("Otuo MFB").active(true).build(),
                BankInfo.builder().bankCode("100002").bankName("Paga").active(true).build(),
                BankInfo.builder().bankCode("70008").bankName("Page MFBank").active(true).build(),
                BankInfo.builder().bankCode("100033").bankName("PALMPAY").active(true).build(),
                BankInfo.builder().bankCode("30").bankName("Parallex Bank").active(true).build(),
                BankInfo.builder().bankCode("100003").bankName("Parkway-ReadyCash").active(true).build(),
                BankInfo.builder().bankCode("90004").bankName("Parralex").active(true).build(),
                BankInfo.builder().bankCode("90680").bankName("Pathfinder Microfinance Bank Limited").active(true).build(),
                BankInfo.builder().bankCode("90317").bankName("PATRICKGOLD MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("100004").bankName("Paycom(OPay)").active(true).build(),
                BankInfo.builder().bankCode("90402").bankName("Peace MFB").active(true).build(),
                BankInfo.builder().bankCode("90137").bankName("Pecan Trust Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90196").bankName("Pennywise Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("90135").bankName("Personal Trust Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90165").bankName("Petra Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("70013").bankName("PLATINUM MORTGAGE BANK").active(true).build(),
                BankInfo.builder().bankCode("000008").bankName("POLARIS BANK").active(true).build(),
                BankInfo.builder().bankCode("090534").bankName("POLYIBADAN MFB").active(true).build(),
                BankInfo.builder().bankCode("000031").bankName("Premium Trust Bank").active(true).build(),
                BankInfo.builder().bankCode("90499").bankName("Pristine Divitis MFB").active(true).build(),
                BankInfo.builder().bankCode("090689").bankName("PROSPECTS MFB").active(true).build(),
                BankInfo.builder().bankCode("000023").bankName("Providus Bank ").active(true).build(),
                BankInfo.builder().bankCode("090690").bankName("PRUDENT MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("090303").bankName("Purple Money MFB").active(true).build(),
                BankInfo.builder().bankCode("90657").bankName("Pyramid MFB").active(true).build(),
                BankInfo.builder().bankCode("090569").bankName("QUBE MFB").active(true).build(),
                BankInfo.builder().bankCode("90261").bankName("QuickFund Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("90170").bankName("Rahama MFB").active(true).build(),
                BankInfo.builder().bankCode("000024").bankName("Rand Merchant Bank").active(true).build(),
                BankInfo.builder().bankCode("90496").bankName("Randalpha Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("70011").bankName("Refuge Mortgage Bank").active(true).build(),
                BankInfo.builder().bankCode("90125").bankName("REGENT MFB").active(true).build(),
                BankInfo.builder().bankCode("090463").bankName("Rehoboth Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90173").bankName("Reliance MFB").active(true).build(),
                BankInfo.builder().bankCode("90198").bankName("Renmoney microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("90666").bankName("Revelation MFB").active(true).build(),
                BankInfo.builder().bankCode("90449").bankName("Rex MFB").active(true).build(),
                BankInfo.builder().bankCode("90132").bankName("RICHWAY MFB").active(true).build(),
                BankInfo.builder().bankCode("90547").bankName("ROCKSHIELD MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90622").bankName("Royal Blue MFB").active(true).build(),
                BankInfo.builder().bankCode("90138").bankName("Royal Exchange Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("090535").bankName("RSU MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("090286").bankName("Safe Haven MFB").active(true).build(),
                BankInfo.builder().bankCode("90006").bankName("SafeTrust").active(true).build(),
                BankInfo.builder().bankCode("90140").bankName("Sagamu Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90112").bankName("Seed Capital Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90369").bankName("Seedvest Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("90502").bankName("Shalom MFB").active(true).build(),
                BankInfo.builder().bankCode("000034").bankName("Signature Bank").active(true).build(),
                BankInfo.builder().bankCode("090506").bankName("Solid Allianze MFB").active(true).build(),
                BankInfo.builder().bankCode("90641").bankName("Source MFB").active(true).build(),
                BankInfo.builder().bankCode("90325").bankName("Sparkle MFB").active(true).build(),
                BankInfo.builder().bankCode("90436").bankName("Spectrum MFB").active(true).build(),
                BankInfo.builder().bankCode("000012").bankName("StanbicIBTC Bank").active(true).build(),
                BankInfo.builder().bankCode("100007").bankName("StanbicMobileMoney").active(true).build(),
                BankInfo.builder().bankCode("000021").bankName("StandardChartered").active(true).build(),
                BankInfo.builder().bankCode("90162").bankName("Stanford MFB").active(true).build(),
                BankInfo.builder().bankCode("90262").bankName("Stellas MFB").active(true).build(),
                BankInfo.builder().bankCode("000001").bankName("Sterling Bank").active(true).build(),
                BankInfo.builder().bankCode("90644").bankName("Suntop MFB").active(true).build(),
                BankInfo.builder().bankCode("000022").bankName("SUNTRUST BANK").active(true).build(),
                BankInfo.builder().bankCode("090446").bankName("Support MFB").active(true).build(),
                BankInfo.builder().bankCode("100023").bankName("TagPay").active(true).build(),
                BankInfo.builder().bankCode("000026").bankName("TAJ Bank").active(true).build(),
                BankInfo.builder().bankCode("80002").bankName("Taj_PinsPay").active(true).build(),
                BankInfo.builder().bankCode("90426").bankName("Tangerine Money").active(true).build(),
                BankInfo.builder().bankCode("90115").bankName("TCF").active(true).build(),
                BankInfo.builder().bankCode("100010").bankName("TeasyMobile").active(true).build(),
                BankInfo.builder().bankCode("100039").bankName("Titan Paystack").active(true).build(),
                BankInfo.builder().bankCode("25").bankName("Titan Trust Bank").active(true).build(),
                BankInfo.builder().bankCode("90663").bankName("Treasures MFB").active(true).build(),
                BankInfo.builder().bankCode("90146").bankName("Trident Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("050023").bankName("TRIVES FINANCE COMPANY LTD").active(true).build(),
                BankInfo.builder().bankCode("90005").bankName("Trustbond").active(true).build(),
                BankInfo.builder().bankCode("90315").bankName("U And C MFB").active(true).build(),
                BankInfo.builder().bankCode("090403").bankName("UDA MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("090514").bankName("Umuchinemere Procredit MFB").active(true).build(),
                BankInfo.builder().bankCode("90652").bankName("Umuchukwu MFB").active(true).build(),
                BankInfo.builder().bankCode("90266").bankName("Uniben Microfinance bank").active(true).build(),
                BankInfo.builder().bankCode("90193").bankName("Unical MFB").active(true).build(),
                BankInfo.builder().bankCode("000018").bankName("Union Bank").active(true).build(),
                BankInfo.builder().bankCode("000004").bankName("United Bank for Africa").active(true).build(),
                BankInfo.builder().bankCode("000011").bankName("Unity Bank").active(true).build(),
                BankInfo.builder().bankCode("90338").bankName("Uniuyo MFB").active(true).build(),
                BankInfo.builder().bankCode("090619").bankName("URE MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90453").bankName("UZONDU MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("050020").bankName("VALE FINANCE LIMITED").active(true).build(),
                BankInfo.builder().bankCode("90123").bankName("Verite Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("090110").bankName("VFD MFB").active(true).build(),
                BankInfo.builder().bankCode("90150").bankName("Virtue MFB").active(true).build(),
                BankInfo.builder().bankCode("90139").bankName("Visa Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("100012").bankName("VTNetworks").active(true).build(),
                BankInfo.builder().bankCode("090590").bankName("Waya Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("000017").bankName("Wema Bank").active(true).build(),
                BankInfo.builder().bankCode("90120").bankName("WETLAND MFB").active(true).build(),
                BankInfo.builder().bankCode("90253").bankName("WUDIL MICROFINANCE BANK").active(true).build(),
                BankInfo.builder().bankCode("90124").bankName("XSLNCE Microfinance Bank").active(true).build(),
                BankInfo.builder().bankCode("090142").bankName("Yes MFB").active(true).build(),
                BankInfo.builder().bankCode("000015").bankName("ZENITH BANK PLC").active(true).build(),
                BankInfo.builder().bankCode("100034").bankName("Zenith Eazy Wallet").active(true).build(),
                BankInfo.builder().bankCode("100018").bankName("ZenithMobile").active(true).build(),
                BankInfo.builder().bankCode("100025").bankName("Zinternet - KongaPay").active(true).build(),
                BankInfo.builder().bankCode(INTERNAL_BANK_CODE).bankName("Fulus Pay").active(true).build()
        );

        bankInfoRepository.saveAll(banks);
        log.info("Initialized {} banks", banks.size());
    }

    /**
     * Get list of all active banks
     */
    public BankListResponse getBankList() {
        log.info("Fetching bank list");

        List<BankListResponse.BankDTO> banks = bankInfoRepository.findByActiveTrue()
                .stream()
                .map(bank -> BankListResponse.BankDTO.builder()
                        .bankCode(bank.getBankCode())
                        .bankName(bank.getBankName())
                        .build())
                .collect(Collectors.toList());

        log.info("Retrieved {} active banks", banks.size());
        return BankListResponse.success(banks);
    }

    /**
     * Name enquiry for account verification
     */
    public NameEnquiryResponse nameEnquiry(NameEnquiryRequest request, UUID currentUserId) {
        log.info("Name enquiry for account: {} at bank: {}", request.getAccountNumber(), request.getBankCode());

        // Verify bank exists
        BankInfo bank = bankInfoRepository.findByBankCode(request.getBankCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid bank code"));

        // Check if it's internal transfer (Fulus Pay)
        if (INTERNAL_BANK_CODE.equals(request.getBankCode())) {
            return performInternalNameEnquiry(request.getAccountNumber(), bank, currentUserId);
        }

        // For other banks, simulate external name enquiry
        return performExternalNameEnquiry(request.getAccountNumber(), bank);
    }

    /**
     * Perform name enquiry for Fulus Pay accounts
     */
    private NameEnquiryResponse performInternalNameEnquiry(String accountNumber, BankInfo bank, UUID currentUserId) {
        // Try to find by account number first, then by phone number
        User user = userRepository.findByAccountNumber(accountNumber)
                .or(() -> userRepository.findByPhoneNumber(accountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Prevent name enquiry on own account
        if (user.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Cannot perform name enquiry on your own account");
        }

        log.info("Internal name enquiry successful for account/phone: {}", accountNumber);

        return NameEnquiryResponse.success(
                user.getAccountNumber(), // Return the actual account number
                user.getName(),
                bank.getBankCode(),
                bank.getBankName()
        );
    }

    /**
     * Simulate external bank name enquiry
     * In production, this would call an actual banking API (e.g., Paystack, Flutterwave)
     */
    private NameEnquiryResponse performExternalNameEnquiry(String accountNumber, BankInfo bank) {
        log.info("Simulating external name enquiry for account: {} at {}", accountNumber, bank.getBankName());

        // TODO: In production, integrate with real banking API
        // For now, return a simulated response with a random Nigerian name
        String simulatedName = NIGERIAN_NAMES.get(random.nextInt(NIGERIAN_NAMES.size()));

        return NameEnquiryResponse.success(
                accountNumber,
                simulatedName,
                bank.getBankCode(),
                bank.getBankName()
        );
    }

    /**
     * Check if bank code is internal (Fulus Pay)
     */
    public boolean isInternalBank(String bankCode) {
        return INTERNAL_BANK_CODE.equals(bankCode);
    }
}
