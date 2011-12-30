package org.systemsbiology.addama.sandbox.httpproxy;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientException;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientResponseException;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.ResponseCallback;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.services.proxy.controllers.SimpleProxyController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class SimpleProxyControllerTest {
    private SimpleProxyController controller;
    private MockHttpClientTemplate httpClientTemplate;

    @Before
    public void setUp() throws Exception {
        httpClientTemplate = new MockHttpClientTemplate();

        MockServletContext msc = new MockServletContext();
        msc.setContextPath("simpleProxyControllerTest");
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setServletContext(msc);

        controller = new SimpleProxyController();
        controller.setHttpClientTemplate(httpClientTemplate);
        controller.setServiceConfig(serviceConfig);
    }

    @Test
    public void requestParams() throws Exception {
        MockHttpServletRequest incoming = new MockHttpServletRequest();
        incoming.setMethod("POST");
        incoming.setRequestURI("/addama/applications/edgelimma/EdgeLimma_selectFiles.pl");

        String inQueryString = "attr0=time&attr1=treatment&attr2=&cel100_0=24&cel100_1=clean&cel100_2=&cel101_0=24&cel101_1=clean&cel101_2=&cel102_0=24&cel102_1=clean&cel102_2=&cel103_0=24&cel103_1=clean&cel103_2=&cel104_0=24&cel104_1=clean&cel104_2=&cel105_0=24&cel105_1=clean&cel105_2=&cel106_0=24&cel106_1=infected&cel106_2=&cel107_0=24&cel107_1=infected&cel107_2=&cel108_0=24&cel108_1=infected&cel108_2=&cel109_0=24&cel109_1=infected&cel109_2=&cel10_0=48&cel10_1=clean&cel10_2=&cel110_0=24&cel110_1=infected&cel110_2=&cel111_0=24&cel111_1=infected&cel111_2=&cel112_0=24&cel112_1=infected&cel112_2=&cel113_0=24&cel113_1=infected&cel113_2=&cel114_0=24&cel114_1=infected&cel114_2=&cel115_0=24&cel115_1=infected&cel115_2=&cel116_0=48&cel116_1=clean&cel116_2=&cel117_0=48&cel117_1=clean&cel117_2=&cel118_0=48&cel118_1=clean&cel118_2=&cel119_0=48&cel119_1=clean&cel119_2=&cel11_0=48&cel11_1=infected&cel11_2=&cel120_0=48&cel120_1=clean&cel120_2=&cel121_0=48&cel121_1=clean&cel121_2=&cel122_0=48&cel122_1=clean&cel122_2=&cel123_0=48&cel123_1=clean&cel123_2=&cel124_0=48&cel124_1=clean&cel124_2=&cel125_0=48&cel125_1=clean&cel125_2=&cel126_0=48&cel126_1=infected&cel126_2=&cel127_0=48&cel127_1=infected&cel127_2=&cel128_0=48&cel128_1=infected&cel128_2=&cel129_0=48&cel129_1=infected&cel129_2=&cel12_0=4&cel12_1=clean&cel12_2=&cel130_0=48&cel130_1=infected&cel130_2=&cel131_0=48&cel131_1=infected&cel131_2=&cel132_0=48&cel132_1=infected&cel132_2=&cel133_0=48&cel133_1=infected&cel133_2=&cel134_0=48&cel134_1=infected&cel134_2=&cel135_0=48&cel135_1=infected&cel135_2=&cel136_0=4&cel136_1=clean&cel136_2=&cel137_0=4&cel137_1=clean&cel137_2=&cel138_0=4&cel138_1=clean&cel138_2=&cel139_0=4&cel139_1=clean&cel139_2=&cel13_0=6&cel13_1=clean&cel13_2=&cel140_0=4&cel140_1=clean&cel140_2=&cel141_0=4&cel141_1=clean&cel141_2=&cel142_0=4&cel142_1=clean&cel142_2=&cel143_0=4&cel143_1=clean&cel143_2=&cel144_0=4&cel144_1=clean&cel144_2=&cel145_0=4&cel145_1=clean&cel145_2=&cel146_0=6&cel146_1=clean&cel146_2=&cel147_0=6&cel147_1=clean&cel147_2=&cel148_0=6&cel148_1=clean&cel148_2=&cel149_0=6&cel149_1=clean&cel149_2=&cel14_0=8&cel14_1=clean&cel14_2=&cel150_0=6&cel150_1=clean&cel150_2=&cel151_0=6&cel151_1=clean&cel151_2=&cel152_0=6&cel152_1=clean&cel152_2=&cel153_0=6&cel153_1=clean&cel153_2=&cel154_0=6&cel154_1=clean&cel154_2=&cel155_0=6&cel155_1=clean&cel155_2=&cel156_0=8&cel156_1=clean&cel156_2=&cel157_0=8&cel157_1=clean&cel157_2=&cel158_0=8&cel158_1=clean&cel158_2=&cel159_0=8&cel159_1=clean&cel159_2=&cel15_0=10&cel15_1=clean&cel15_2=&cel160_0=8&cel160_1=clean&cel160_2=&cel161_0=8&cel161_1=clean&cel161_2=&cel162_0=8&cel162_1=clean&cel162_2=&cel163_0=8&cel163_1=clean&cel163_2=&cel164_0=8&cel164_1=clean&cel164_2=&cel165_0=8&cel165_1=clean&cel165_2=&cel166_0=10&cel166_1=clean&cel166_2=&cel167_0=10&cel167_1=clean&cel167_2=&cel168_0=10&cel168_1=clean&cel168_2=&cel169_0=10&cel169_1=clean&cel169_2=&cel16_0=4&cel16_1=infected&cel16_2=&cel170_0=10&cel170_1=clean&cel170_2=&cel171_0=10&cel171_1=clean&cel171_2=&cel172_0=10&cel172_1=clean&cel172_2=&cel173_0=10&cel173_1=clean&cel173_2=&cel174_0=10&cel174_1=clean&cel174_2=&cel175_0=10&cel175_1=clean&cel175_2=&cel176_0=4&cel176_1=infected&cel176_2=&cel177_0=4&cel177_1=infected&cel177_2=&cel178_0=4&cel178_1=infected&cel178_2=&cel179_0=4&cel179_1=infected&cel179_2=&cel17_0=6&cel17_1=infected&cel17_2=&cel180_0=4&cel180_1=infected&cel180_2=&cel181_0=4&cel181_1=infected&cel181_2=&cel182_0=4&cel182_1=infected&cel182_2=&cel183_0=4&cel183_1=infected&cel183_2=&cel184_0=4&cel184_1=infected&cel184_2=&cel185_0=4&cel185_1=infected&cel185_2=&cel186_0=6&cel186_1=infected&cel186_2=&cel187_0=6&cel187_1=infected&cel187_2=&cel188_0=6&cel188_1=infected&cel188_2=&cel189_0=6&cel189_1=infected&cel189_2=&cel18_0=8&cel18_1=infected&cel18_2=&cel190_0=6&cel190_1=infected&cel190_2=&cel191_0=6&cel191_1=infected&cel191_2=&cel192_0=6&cel192_1=infected&cel192_2=&cel193_0=6&cel193_1=infected&cel193_2=&cel194_0=6&cel194_1=infected&cel194_2=&cel195_0=6&cel195_1=infected&cel195_2=&cel196_0=8&cel196_1=infected&cel196_2=&cel197_0=8&cel197_1=infected&cel197_2=&cel198_0=8&cel198_1=infected&cel198_2=&cel199_0=8&cel199_1=infected&cel199_2=&cel19_0=10&cel19_1=infected&cel19_2=&cel200_0=8&cel200_1=infected&cel200_2=&cel201_0=8&cel201_1=infected&cel201_2=&cel202_0=8&cel202_1=infected&cel202_2=&cel203_0=8&cel203_1=infected&cel203_2=&cel204_0=8&cel204_1=infected&cel204_2=&cel205_0=8&cel205_1=infected&cel205_2=&cel206_0=10&cel206_1=infected&cel206_2=&cel207_0=10&cel207_1=infected&cel207_2=&cel208_0=10&cel208_1=infected&cel208_2=&cel209_0=10&cel209_1=infected&cel209_2=&cel20_0=12&cel20_1=clean&cel20_2=&cel210_0=10&cel210_1=infected&cel210_2=&cel211_0=10&cel211_1=infected&cel211_2=&cel212_0=10&cel212_1=infected&cel212_2=&cel213_0=10&cel213_1=infected&cel213_2=&cel214_0=10&cel214_1=infected&cel214_2=&cel215_0=10&cel215_1=infected&cel215_2=&cel216_0=12&cel216_1=clean&cel216_2=&cel217_0=12&cel217_1=clean&cel217_2=&cel218_0=12&cel218_1=clean&cel218_2=&cel219_0=12&cel219_1=clean&cel219_2=&cel21_0=15&cel21_1=clean&cel21_2=&cel220_0=12&cel220_1=clean&cel220_2=&cel221_0=12&cel221_1=clean&cel221_2=&cel222_0=12&cel222_1=clean&cel222_2=&cel223_0=12&cel223_1=clean&cel223_2=&cel224_0=12&cel224_1=clean&cel224_2=&cel225_0=12&cel225_1=clean&cel225_2=&cel226_0=15&cel226_1=clean&cel226_2=&cel227_0=15&cel227_1=clean&cel227_2=&cel228_0=15&cel228_1=clean&cel228_2=&cel229_0=15&cel229_1=clean&cel229_2=&cel22_0=18&cel22_1=clean&cel22_2=&cel230_0=15&cel230_1=clean&cel230_2=&cel231_0=15&cel231_1=clean&cel231_2=&cel232_0=15&cel232_1=clean&cel232_2=&cel233_0=15&cel233_1=clean&cel233_2=&cel234_0=15&cel234_1=clean&cel234_2=&cel235_0=15&cel235_1=clean&cel235_2=&cel236_0=18&cel236_1=clean&cel236_2=&cel237_0=18&cel237_1=clean&cel237_2=&cel238_0=18&cel238_1=clean&cel238_2=&cel239_0=18&cel239_1=clean&cel239_2=&cel23_0=21&cel23_1=clean&cel23_2=&cel240_0=18&cel240_1=clean&cel240_2=&cel241_0=18&cel241_1=clean&cel241_2=&cel242_0=18&cel242_1=clean&cel242_2=&cel243_0=18&cel243_1=clean&cel243_2=&cel244_0=18&cel244_1=clean&cel244_2=&cel245_0=18&cel245_1=clean&cel245_2=&cel246_0=21&cel246_1=clean&cel246_2=&cel247_0=21&cel247_1=clean&cel247_2=&cel248_0=21&cel248_1=clean&cel248_2=&cel249_0=21&cel249_1=clean&cel249_2=&cel24_0=12&cel24_1=infected&cel24_2=&cel250_0=21&cel250_1=clean&cel250_2=&cel251_0=21&cel251_1=clean&cel251_2=&cel252_0=21&cel252_1=clean&cel252_2=&cel253_0=21&cel253_1=clean&cel253_2=&cel254_0=21&cel254_1=clean&cel254_2=&cel255_0=21&cel255_1=clean&cel255_2=&cel256_0=12&cel256_1=infected&cel256_2=&cel257_0=12&cel257_1=infected&cel257_2=&cel258_0=12&cel258_1=infected&cel258_2=&cel259_0=12&cel259_1=infected&cel259_2=&cel25_0=15&cel25_1=infected&cel25_2=&cel260_0=12&cel260_1=infected&cel260_2=&cel261_0=12&cel261_1=infected&cel261_2=&cel262_0=12&cel262_1=infected&cel262_2=&cel263_0=12&cel263_1=infected&cel263_2=&cel264_0=12&cel264_1=infected&cel264_2=&cel265_0=12&cel265_1=infected&cel265_2=&cel266_0=15&cel266_1=infected&cel266_2=&cel267_0=15&cel267_1=infected&cel267_2=&cel268_0=15&cel268_1=infected&cel268_2=&cel269_0=15&cel269_1=infected&cel269_2=&cel26_0=18&cel26_1=infected&cel26_2=&cel270_0=15&cel270_1=infected&cel270_2=&cel271_0=15&cel271_1=infected&cel271_2=&cel272_0=15&cel272_1=infected&cel272_2=&cel273_0=15&cel273_1=infected&cel273_2=&cel274_0=15&cel274_1=infected&cel274_2=&cel275_0=15&cel275_1=infected&cel275_2=&cel276_0=18&cel276_1=infected&cel276_2=&cel277_0=18&cel277_1=infected&cel277_2=&cel278_0=18&cel278_1=infected&cel278_2=&cel279_0=18&cel279_1=infected&cel279_2=&cel27_0=21&cel27_1=infected&cel27_2=&cel280_0=18&cel280_1=infected&cel280_2=&cel281_0=18&cel281_1=infected&cel281_2=&cel282_0=18&cel282_1=infected&cel282_2=&cel283_0=18&cel283_1=infected&cel283_2=&cel284_0=18&cel284_1=infected&cel284_2=&cel285_0=18&cel285_1=infected&cel285_2=&cel286_0=21&cel286_1=infected&cel286_2=&cel287_0=21&cel287_1=infected&cel287_2=&cel288_0=21&cel288_1=infected&cel288_2=&cel289_0=21&cel289_1=infected&cel289_2=&cel28_0=35&cel28_1=clean&cel28_2=&cel290_0=21&cel290_1=infected&cel290_2=&cel291_0=21&cel291_1=infected&cel291_2=&cel292_0=21&cel292_1=infected&cel292_2=&cel293_0=21&cel293_1=infected&cel293_2=&cel294_0=21&cel294_1=infected&cel294_2=&cel295_0=21&cel295_1=infected&cel295_2=&cel296_0=35&cel296_1=clean&cel296_2=&cel297_0=35&cel297_1=clean&cel297_2=&cel298_0=35&cel298_1=clean&cel298_2=&cel299_0=35&cel299_1=clean&cel299_2=&cel29_0=38&cel29_1=clean&cel29_2=&cel2_0=1&cel2_1=clean&cel2_2=&cel300_0=35&cel300_1=clean&cel300_2=&cel301_0=35&cel301_1=clean&cel301_2=&cel302_0=35&cel302_1=clean&cel302_2=&cel303_0=35&cel303_1=clean&cel303_2=&cel304_0=35&cel304_1=clean&cel304_2=&cel305_0=35&cel305_1=clean&cel305_2=&cel306_0=38&cel306_1=clean&cel306_2=&cel307_0=38&cel307_1=clean&cel307_2=&cel308_0=38&cel308_1=clean&cel308_2=&cel309_0=38&cel309_1=clean&cel309_2=&cel30_0=41&cel30_1=clean&cel30_2=&cel310_0=38&cel310_1=clean&cel310_2=&cel311_0=38&cel311_1=clean&cel311_2=&cel312_0=38&cel312_1=clean&cel312_2=&cel313_0=38&cel313_1=clean&cel313_2=&cel314_0=38&cel314_1=clean&cel314_2=&cel315_0=38&cel315_1=clean&cel315_2=&cel316_0=41&cel316_1=clean&cel316_2=&cel317_0=41&cel317_1=clean&cel317_2=&cel318_0=41&cel318_1=clean&cel318_2=&cel319_0=41&cel319_1=clean&cel319_2=&cel31_0=45&cel31_1=clean&cel31_2=&cel320_0=41&cel320_1=clean&cel320_2=&cel321_0=41&cel321_1=clean&cel321_2=&cel322_0=41&cel322_1=clean&cel322_2=&cel323_0=41&cel323_1=clean&cel323_2=&cel324_0=41&cel324_1=clean&cel324_2=&cel325_0=41&cel325_1=clean&cel325_2=&cel326_0=45&cel326_1=clean&cel326_2=&cel327_0=45&cel327_1=clean&cel327_2=&cel328_0=45&cel328_1=clean&cel328_2=&cel329_0=45&cel329_1=clean&cel329_2=&cel32_0=35&cel32_1=infected&cel32_2=&cel330_0=45&cel330_1=clean&cel330_2=&cel331_0=45&cel331_1=clean&cel331_2=&cel332_0=45&cel332_1=clean&cel332_2=&cel333_0=45&cel333_1=clean&cel333_2=&cel334_0=45&cel334_1=clean&cel334_2=&cel335_0=45&cel335_1=clean&cel335_2=&cel336_0=35&cel336_1=infected&cel336_2=&cel337_0=35&cel337_1=infected&cel337_2=&cel338_0=35&cel338_1=infected&cel338_2=&cel339_0=35&cel339_1=infected&cel339_2=&cel33_0=38&cel33_1=infected&cel33_2=&cel340_0=35&cel340_1=infected&cel340_2=&cel341_0=35&cel341_1=infected&cel341_2=&cel342_0=35&cel342_1=infected&cel342_2=&cel343_0=35&cel343_1=infected&cel343_2=&cel344_0=35&cel344_1=infected&cel344_2=&cel345_0=35&cel345_1=infected&cel345_2=&cel346_0=38&cel346_1=infected&cel346_2=&cel347_0=38&cel347_1=infected&cel347_2=&cel348_0=38&cel348_1=infected&cel348_2=&cel349_0=38&cel349_1=infected&cel349_2=&cel34_0=41&cel34_1=infected&cel34_2=&cel350_0=38&cel350_1=infected&cel350_2=&cel351_0=38&cel351_1=infected&cel351_2=&cel352_0=38&cel352_1=infected&cel352_2=&cel353_0=38&cel353_1=infected&cel353_2=&cel354_0=38&cel354_1=infected&cel354_2=&cel355_0=38&cel355_1=infected&cel355_2=&cel356_0=41&cel356_1=infected&cel356_2=&cel357_0=41&cel357_1=infected&cel357_2=&cel358_0=41&cel358_1=infected&cel358_2=&cel359_0=41&cel359_1=infected&cel359_2=&cel35_0=45&cel35_1=infected&cel35_2=&cel360_0=41&cel360_1=infected&cel360_2=&cel361_0=41&cel361_1=infected&cel361_2=&cel362_0=41&cel362_1=infected&cel362_2=&cel363_0=41&cel363_1=infected&cel363_2=&cel364_0=41&cel364_1=infected&cel364_2=&cel365_0=41&cel365_1=infected&cel365_2=&cel366_0=45&cel366_1=infected&cel366_2=&cel367_0=45&cel367_1=infected&cel367_2=&cel368_0=45&cel368_1=infected&cel368_2=&cel369_0=45&cel369_1=infected&cel369_2=&cel36_0=1&cel36_1=clean&cel36_2=&cel370_0=45&cel370_1=infected&cel370_2=&cel371_0=45&cel371_1=infected&cel371_2=&cel372_0=45&cel372_1=infected&cel372_2=&cel373_0=45&cel373_1=infected&cel373_2=&cel374_0=45&cel374_1=infected&cel374_2=&cel375_0=45&cel375_1=infected&cel375_2=&cel37_0=1&cel37_1=clean&cel37_2=&cel38_0=1&cel38_1=clean&cel38_2=&cel39_0=1&cel39_1=clean&cel39_2=&cel3_0=1&cel3_1=infected&cel3_2=&cel40_0=1&cel40_1=clean&cel40_2=&cel41_0=1&cel41_1=clean&cel41_2=&cel42_0=1&cel42_1=clean&cel42_2=&cel43_0=1&cel43_1=clean&cel43_2=&cel44_0=1&cel44_1=clean&cel44_2=&cel45_0=1&cel45_1=clean&cel45_2=&cel46_0=1&cel46_1=infected&cel46_2=&cel47_0=1&cel47_1=infected&cel47_2=&cel48_0=1&cel48_1=infected&cel48_2=&cel49_0=1&cel49_1=infected&cel49_2=&cel4_0=2&cel4_1=clean&cel4_2=&cel50_0=1&cel50_1=infected&cel50_2=&cel51_0=1&cel51_1=infected&cel51_2=&cel52_0=1&cel52_1=infected&cel52_2=&cel53_0=1&cel53_1=infected&cel53_2=&cel54_0=1&cel54_1=infected&cel54_2=&cel55_0=1&cel55_1=infected&cel55_2=&cel56_0=2&cel56_1=clean&cel56_2=&cel57_0=2&cel57_1=clean&cel57_2=&cel58_0=2&cel58_1=clean&cel58_2=&cel59_0=2&cel59_1=clean&cel59_2=&cel5_0=2&cel5_1=infected&cel5_2=&cel60_0=2&cel60_1=clean&cel60_2=&cel61_0=2&cel61_1=clean&cel61_2=&cel62_0=2&cel62_1=clean&cel62_2=&cel63_0=2&cel63_1=clean&cel63_2=&cel64_0=2&cel64_1=clean&cel64_2=&cel65_0=2&cel65_1=clean&cel65_2=&cel66_0=2&cel66_1=infected&cel66_2=&cel67_0=2&cel67_1=infected&cel67_2=&cel68_0=2&cel68_1=infected&cel68_2=&cel69_0=2&cel69_1=infected&cel69_2=&cel6_0=12&cel6_1=clean&cel6_2=&cel70_0=2&cel70_1=infected&cel70_2=&cel71_0=2&cel71_1=infected&cel71_2=&cel72_0=2&cel72_1=infected&cel72_2=&cel73_0=2&cel73_1=infected&cel73_2=&cel74_0=2&cel74_1=infected&cel74_2=&cel75_0=2&cel75_1=infected&cel75_2=&cel76_0=12&cel76_1=clean&cel76_2=&cel77_0=12&cel77_1=clean&cel77_2=&cel78_0=12&cel78_1=clean&cel78_2=&cel79_0=12&cel79_1=clean&cel79_2=&cel7_0=12&cel7_1=infected&cel7_2=&cel80_0=12&cel80_1=clean&cel80_2=&cel81_0=12&cel81_1=clean&cel81_2=&cel82_0=12&cel82_1=clean&cel82_2=&cel83_0=12&cel83_1=clean&cel83_2=&cel84_0=12&cel84_1=clean&cel84_2=&cel85_0=12&cel85_1=clean&cel85_2=&cel86_0=12&cel86_1=infected&cel86_2=&cel87_0=12&cel87_1=infected&cel87_2=&cel88_0=12&cel88_1=infected&cel88_2=&cel89_0=12&cel89_1=infected&cel89_2=&cel8_0=24&cel8_1=clean&cel8_2=&cel90_0=12&cel90_1=infected&cel90_2=&cel91_0=12&cel91_1=infected&cel91_2=&cel92_0=12&cel92_1=infected&cel92_2=&cel93_0=12&cel93_1=infected&cel93_2=&cel94_0=12&cel94_1=infected&cel94_2=&cel95_0=12&cel95_1=infected&cel95_2=&cel96_0=24&cel96_1=clean&cel96_2=&cel97_0=24&cel97_1=clean&cel97_2=&cel98_0=24&cel98_1=clean&cel98_2=&cel99_0=24&cel99_1=clean&cel99_2=&cel9_0=24&cel9_1=infected&cel9_2=&file=%2Faddama%2Fworkspaces%2Fedgelimma%2Fakeller%40systemsbiology.org%2F3429387%2FNS_Tg4053_RML_comb5tp_29k_replicates_0.5stdev_sim2.txt&index_col=1&metafile=%2Faddama%2Fworkspaces%2Fedgelimma%2Fakeller%40systemsbiology.org%2FNS_Tg4053_RML_comb5tp_29k_replicates_0.5stdev_sim2.txt_metadata.txt";
        Map<String, List<String>> inParams = getParams(inQueryString);
        for (Map.Entry<String, List<String>> entry : inParams.entrySet()) {
            List<String> values = entry.getValue();
            incoming.addParameter(entry.getKey(), values.toArray(new String[values.size()]));
        }

        controller.proxy(incoming, new MockHttpServletResponse());

        HttpMethod method = httpClientTemplate.getMethod();
        assertEquals(method.getClass().getName(), PostMethod.class.getName());

        PostMethod outgoing = (PostMethod) method;

        assertEquals("http://example.com:8354/cgi-bin/EdgeLimma_selectFiles.pl", outgoing.getURI().toString());

        Map<String, List<String>> outParams = getParamsFromNvp(outgoing.getParameters());

        assertEquals(inParams.size(), outParams.size());
        assertEquals(inParams, outParams);

        for (Map.Entry<String, List<String>> entry : inParams.entrySet()) {
            String key = entry.getKey();
            List<String> invalues = entry.getValue();
            List<String> outvalues = outParams.get(key);
            assertTrue(outvalues.containsAll(invalues));
            assertTrue(invalues.containsAll(outvalues));
        }
    }

    private Map<String, List<String>> getParamsFromNvp(NameValuePair[] nvps) {
        Map<String, List<String>> params = new HashMap<String, List<String>>();

        for (NameValuePair nvp : nvps) {
            String key = nvp.getName();
            String value = nvp.getValue();
            assertNotNull(value);
//            assertNotSame("", value);
            List<String> vals = params.get(key);
            if (vals == null) {
                vals = new ArrayList<String>();
                params.put(key, vals);
            }
            vals.add(value);
        }
        return params;
    }

    private Map<String, List<String>> getParams(String queryString) {
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyval = pair.split("=");
            List<String> vals = params.get(keyval[0]);
            if (vals == null) {
                vals = new ArrayList<String>();
                params.put(keyval[0], vals);
            }
            if (keyval.length == 2) {
                vals.add(keyval[1]);
            } else {
                vals.add("");
            }
        }
        return params;
    }

    private class MockHttpClientTemplate extends HttpClientTemplate {
        private HttpMethod method;

        public Object executeMethod(HttpMethod method, ResponseCallback responseCallback) throws HttpClientException, HttpClientResponseException {
            this.method = method;
            return null;
        }

        public HttpMethod getMethod() {
            return method;
        }
    }
}
