/**

 *Copyright (C) 2012-2013  Wikimedia Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */



package org.wikimedia.analytics.kraken.pig;

import org.apache.pig.EvalFunc;
import org.apache.pig.PigWarning;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.logicalLayer.schema.Schema.FieldSchema;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * In these comments I may refer to any part of the host separated by '.' a
 * subdomain for example: in en.m.wikipedia.org, the subdomains are 'en', 'm',
 * 'wikipedia'.
 */
@Deprecated
public class ParseWikiUrlER extends EvalFunc<Tuple> {
    private static Set<String> languages;
    private static Map<String, String> versions;
    private static Tuple defaultOutput;

    //public ParseWikiUrlER() {
    //this("src/main/resources/languages.txt");
    //}

    /** {@inheritDoc} */
    public final Tuple exec(final Tuple input) throws ExecException {
        /** Method exec() takes a tuple containing a Wikimedia URL and returns
         *  a tuple containing (if possible) it's language, boolean on whether
         *  it's a mobile site, and it's domain name.
         */
        String language = "N/A";
        String version  = "N/A";
        String project = "N/A";

        if (input == null) {
            warn("null input", PigWarning.UDF_WARNING_1);
            return defaultOutput;
        }

        if (defaultOutput == null) {
            defaultOutput = TupleFactory.getInstance().newTuple(3);
            defaultOutput.set(0, language);
            defaultOutput.set(1, null);
            defaultOutput.set(2, version);
        }

        //gets the urlString from the first argument, return if
        String urlString;

        try {
            urlString = (String) input.get(0);
        } catch (Exception e) {
            warn("argument is invalid", PigWarning.UDF_WARNING_1);
            return defaultOutput;
        }

        if (urlString == null) {
            warn("null input", PigWarning.UDF_WARNING_1);
            return defaultOutput;
        }


        //use url class for parsing
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            warn("malformed URL: " + urlString, PigWarning.UDF_WARNING_1);
            return defaultOutput;
        }

        //gets the host
        String host = url.getHost();
        String[] subdomains = host.split("\\.");


        //if subdomains has less than two elements then can't find domain so return
        if (subdomains.length < 2) {
            warn("host name: " + host + " has less than two subdomains", PigWarning.UDF_WARNING_1);
            return defaultOutput;
        }

        //add language codes from languageFile to a hash set
        if (languages == null) {
            languages = new HashSet<String>();
            languages.add("en");
            languages.add("de");
            languages.add("fr");
            languages.add("nl");
            languages.add("it");
            languages.add("es");
            languages.add("pl");
            languages.add("ru");
            languages.add("ja");
            languages.add("pt");
            languages.add("zh");
            languages.add("sv");
            languages.add("vi");
            languages.add("uk");
            languages.add("ca");
            languages.add("no");
            languages.add("fi");
            languages.add("cs");
            languages.add("fa");
            languages.add("hu");
            languages.add("ro");
            languages.add("ko");
            languages.add("ar");
            languages.add("tr");
            languages.add("id");
            languages.add("sk");
            languages.add("eo");
            languages.add("da");
            languages.add("sr");
            languages.add("kk");
            languages.add("lt");
            languages.add("eu");
            languages.add("ms");
            languages.add("he");
            languages.add("bg");
            languages.add("sl");
            languages.add("vo");
            languages.add("hr");
            languages.add("war");
            languages.add("hi");
            languages.add("et");
            languages.add("gl");
            languages.add("az");
            languages.add("nn");
            languages.add("simple");
            languages.add("la");
            languages.add("el");
            languages.add("th");
            languages.add("sh");
            languages.add("oc");
            languages.add("new");
            languages.add("mk");
            languages.add("roa-rup");
            languages.add("ka");
            languages.add("tl");
            languages.add("pms");
            languages.add("ht");
            languages.add("be");
            languages.add("te");
            languages.add("ta");
            languages.add("be-x-old");
            languages.add("uz");
            languages.add("lv");
            languages.add("br");
            languages.add("ceb");
            languages.add("sq");
            languages.add("jv");
            languages.add("mg");
            languages.add("mr");
            languages.add("cy");
            languages.add("lb");
            languages.add("is");
            languages.add("bs");
            languages.add("hy");
            languages.add("my");
            languages.add("yo");
            languages.add("an");
            languages.add("lmo");
            languages.add("ml");
            languages.add("pnb");
            languages.add("fy");
            languages.add("bpy");
            languages.add("af");
            languages.add("bn");
            languages.add("sw");
            languages.add("io");
            languages.add("ne");
            languages.add("gu");
            languages.add("zh-yue");
            languages.add("nds");
            languages.add("ur");
            languages.add("ba");
            languages.add("scn");
            languages.add("ku");
            languages.add("ast");
            languages.add("qu");
            languages.add("su");
            languages.add("diq");
            languages.add("tt");
            languages.add("ga");
            languages.add("ky");
            languages.add("cv");
            languages.add("ia");
            languages.add("nap");
            languages.add("bat-smg");
            languages.add("map-bms");
            languages.add("als");
            languages.add("wa");
            languages.add("kn");
            languages.add("am");
            languages.add("gd");
            languages.add("ckb");
            languages.add("sco");
            languages.add("bug");
            languages.add("tg");
            languages.add("mzn");
            languages.add("zh-min-nan");
            languages.add("yi");
            languages.add("vec");
            languages.add("arz");
            languages.add("hif");
            languages.add("roa-tara");
            languages.add("nah");
            languages.add("os");
            languages.add("sah");
            languages.add("mn");
            languages.add("sa");
            languages.add("pam");
            languages.add("hsb");
            languages.add("li");
            languages.add("mi");
            languages.add("si");
            languages.add("se");
            languages.add("co");
            languages.add("gan");
            languages.add("glk");
            languages.add("bar");
            languages.add("fo");
            languages.add("ilo");
            languages.add("bo");
            languages.add("bcl");
            languages.add("mrj");
            languages.add("fiu-vro");
            languages.add("nds-nl");
            languages.add("ps");
            languages.add("tk");
            languages.add("vls");
            languages.add("gv");
            languages.add("rue");
            languages.add("pa");
            languages.add("dv");
            languages.add("xmf");
            languages.add("pag");
            languages.add("nrm");
            languages.add("kv");
            languages.add("zea");
            languages.add("koi");
            languages.add("km");
            languages.add("rm");
            languages.add("csb");
            languages.add("lad");
            languages.add("udm");
            languages.add("or");
            languages.add("mhr");
            languages.add("mt");
            languages.add("fur");
            languages.add("lij");
            languages.add("wuu");
            languages.add("ug");
            languages.add("pi");
            languages.add("sc");
            languages.add("zh-classical");
            languages.add("frr");
            languages.add("bh");
            languages.add("nov");
            languages.add("ksh");
            languages.add("ang");
            languages.add("so");
            languages.add("stq");
            languages.add("kw");
            languages.add("nv");
            languages.add("hak");
            languages.add("ay");
            languages.add("frp");
            languages.add("vep");
            languages.add("ext");
            languages.add("pcd");
            languages.add("szl");
            languages.add("gag");
            languages.add("gn");
            languages.add("ie");
            languages.add("ln");
            languages.add("haw");
            languages.add("xal");
            languages.add("eml");
            languages.add("pfl");
            languages.add("pdc");
            languages.add("rw");
            languages.add("krc");
            languages.add("crh");
            languages.add("ace");
            languages.add("to");
            languages.add("as");
            languages.add("ce");
            languages.add("kl");
            languages.add("arc");
            languages.add("dsb");
            languages.add("myv");
            languages.add("bjn");
            languages.add("pap");
            languages.add("sn");
            languages.add("tpi");
            languages.add("lbe");
            languages.add("mdf");
            languages.add("wo");
            languages.add("kab");
            languages.add("jbo");
            languages.add("av");
            languages.add("lez");
            languages.add("srn");
            languages.add("cbk-zam");
            languages.add("ty");
            languages.add("bxr");
            languages.add("lo");
            languages.add("kbd");
            languages.add("ab");
            languages.add("tet");
            languages.add("mwl");
            languages.add("ltg");
            languages.add("na");
            languages.add("ig");
            languages.add("kg");
            languages.add("nso");
            languages.add("za");
            languages.add("kaa");
            languages.add("zu");
            languages.add("rmy");
            languages.add("chy");
            languages.add("cu");
            languages.add("tn");
            languages.add("chr");
            languages.add("got");
            languages.add("cdo");
            languages.add("sm");
            languages.add("bi");
            languages.add("mo");
            languages.add("bm");
            languages.add("iu");
            languages.add("pih");
            languages.add("ss");
            languages.add("sd");
            languages.add("pnt");
            languages.add("ee");
            languages.add("om");
            languages.add("ha");
            languages.add("ki");
            languages.add("ti");
            languages.add("ts");
            languages.add("ks");
            languages.add("sg");
            languages.add("ve");
            languages.add("rn");
            languages.add("cr");
            languages.add("ak");
            languages.add("lg");
            languages.add("tum");
            languages.add("dz");
            languages.add("ny");
            languages.add("ik");
            languages.add("ff");
            languages.add("ch");
            languages.add("st");
            languages.add("fj");
            languages.add("tw");
            languages.add("xh");
            languages.add("ng");
            languages.add("ii");
            languages.add("cho");
            languages.add("mh");
            languages.add("aa");
            languages.add("kj");
            languages.add("ho");
            languages.add("mus");
            languages.add("kr");
            languages.add("hz");
        }


        versions = new HashMap<String, String>();
        versions.put("zero", "Z");
        versions.put("m", "M");

        int consumed = 0;

        // find language, if any
        if (languages.contains(subdomains[consumed])) {
            language = subdomains[consumed];
            consumed++;
        }

        // find version, else set to main site
        if (versions.containsKey(subdomains[consumed])) {
            version = versions.get(subdomains[consumed]);
            consumed++;
        } else {
            version = "X";
        }

        project = "";
        for (int i = consumed; i < subdomains.length; i++) {
            if (i < subdomains.length - 1) {
                project += subdomains[i] + '.';
            } else {
                project += subdomains[i];
            }
        }

        //create the tuple for output
        Tuple output = TupleFactory.getInstance().newTuple(3);
        output.set(0, language);
        output.set(1, version);
        output.set(2, project);
        return output;
    }

    /** {@inheritDoc} */
    public Schema outputSchema(final Schema input) {
        Schema inputModel = new Schema(new FieldSchema(null, DataType.CHARARRAY));
        if (!Schema.equals(inputModel, input, true, true)) {
            String msg = "";
            throw new IllegalArgumentException("Expected input schema "
                    + inputModel + ", received schema " + input + msg);
        }
        List<FieldSchema> tupleFields = new LinkedList<FieldSchema>();
        tupleFields.add(new FieldSchema("language", DataType.CHARARRAY));
        tupleFields.add(new FieldSchema("version", DataType.CHARARRAY));
        tupleFields.add(new FieldSchema("project", DataType.CHARARRAY));
        Schema tupleSchema = new Schema(tupleFields);
        try {
            return new Schema(new FieldSchema(null, tupleSchema, DataType.TUPLE));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
