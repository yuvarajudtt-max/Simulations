package com.dtt.simulations.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class PageController {

    @Value("${portal.url}")
    String portalurl;

    @Value("${request.uri}")
    String requestUri;


    @Value("${verify.uri}")
    String verifyUri;

    @Value("${verify.vp.token}")
    String verifyVpToken;

    @Value("${project.name}")
    String projectName;

    @Value("${exchange.currency}")
    String exchangeCurrency;


    @Value("${title.for.claims.pid}")
    String titleForClaimsPid;

    @Value("${title.for.claims.mdl}")
    String titleForClaimsMdl;

    @Value("${attribute.name.mdl}")
    String attributeNameMdl;

    @Value("${attribute.name.pid}")
    String attributeNamePid;


    private static final String VERIFY_URI = "verifyUri";
    private static final String REQUEST_URI = "requestUri";
    private static final String TITLE_FOR_CLAIMS_PID ="titleForClaimsPid";
    private static final String ATTRIBUTE_NAME_PID="attributeNamePid";
    private static final String PROJECT_NAME ="projectName";
    private static final String URL ="url";



    @GetMapping("/")
    public ModelAndView index()
    {
        return new ModelAndView("index");


    }

    @GetMapping("/kepass")
    public ModelAndView kePass()
    {
        return new ModelAndView("index_kenya");


    }

    @GetMapping("/kepass-hotel")
    public ModelAndView kePassHotel(Model model)

    {
        model.addAttribute(URL,portalurl);
        return new ModelAndView("hotel_kenya");


    }

    @GetMapping("/kepass-car")
    public ModelAndView kePassCar(Model model)

    {
        model.addAttribute(URL,portalurl);
        return new ModelAndView("car_kenya");


    }

    @GetMapping("/kepass-bank")
    public ModelAndView kePassBank(Model model)

    {
        model.addAttribute(URL,portalurl);
        return new ModelAndView("bank_kenya");


    }

    @GetMapping("/hotel")
    public ModelAndView hotel(Model model)
    {
        model.addAttribute(URL,portalurl);
        model.addAttribute(VERIFY_URI,verifyUri);
        model.addAttribute(REQUEST_URI,requestUri);
        model.addAttribute(TITLE_FOR_CLAIMS_PID,titleForClaimsPid);
        model.addAttribute(ATTRIBUTE_NAME_PID,attributeNamePid);
        return new ModelAndView("hotel");


    }

    @GetMapping("/moneyExchange")
    public ModelAndView moneyExchange(Model model)
    {
        model.addAttribute(URL,portalurl);
        model.addAttribute(VERIFY_URI,verifyUri);
        model.addAttribute(REQUEST_URI,requestUri);
        model.addAttribute(PROJECT_NAME,projectName);
        model.addAttribute("exchangeCurrency",exchangeCurrency);
        model.addAttribute(TITLE_FOR_CLAIMS_PID,titleForClaimsPid);
        model.addAttribute(ATTRIBUTE_NAME_PID,attributeNamePid);
        return new ModelAndView("moneyExchange");


    }

    @GetMapping("/bank")
    public ModelAndView bank(Model model)
    {
        model.addAttribute(URL,portalurl);
        model.addAttribute(VERIFY_URI,verifyUri);
        model.addAttribute(REQUEST_URI,requestUri);
        model.addAttribute(PROJECT_NAME,projectName);
        model.addAttribute(TITLE_FOR_CLAIMS_PID,titleForClaimsPid);
        model.addAttribute(ATTRIBUTE_NAME_PID,attributeNamePid);
        return new ModelAndView("bank");


    }


    @GetMapping("/carRental")
    public ModelAndView carRental(Model model)
    {
        model.addAttribute(URL,portalurl);
        model.addAttribute(VERIFY_URI,verifyUri);
        model.addAttribute(REQUEST_URI,requestUri);
        model.addAttribute(PROJECT_NAME,projectName);
        model.addAttribute(TITLE_FOR_CLAIMS_PID,titleForClaimsPid);
        model.addAttribute(ATTRIBUTE_NAME_PID,attributeNamePid);
        model.addAttribute("titleForClaimsMdl",titleForClaimsMdl);
        model.addAttribute("attributeNameMdl",attributeNameMdl);
        return new ModelAndView("carRental");


    }
    @GetMapping("/hospital")
    public ModelAndView hospital(Model model)
    {
        model.addAttribute("url",portalurl);
        model.addAttribute(VERIFY_URI,verifyUri);
        model.addAttribute(REQUEST_URI,requestUri);
        return new ModelAndView("hospitalInsurance");


    }

    @GetMapping("/admission")
    public ModelAndView admissionSchool(Model model)
    {
        model.addAttribute(URL,portalurl);
        model.addAttribute(VERIFY_URI,verifyUri);
        model.addAttribute(REQUEST_URI,requestUri);
        return new ModelAndView("admission");


    }

    @GetMapping("/financial-transactions")
    public ModelAndView poa(Model model)
    {
        model.addAttribute(URL,portalurl);
        model.addAttribute(VERIFY_URI,verifyUri);
        model.addAttribute(REQUEST_URI,requestUri);
        model.addAttribute("verifyVpToken",verifyVpToken);
        model.addAttribute(PROJECT_NAME,projectName);
        return new ModelAndView("financialTransaction");


    }


}
