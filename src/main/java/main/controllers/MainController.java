package main.controllers;


import main.services.ContentHandlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @Autowired
    private ContentHandlerService webContentService;


    @GetMapping("/runjob")
    public ResponseEntity runJob(){
        webContentService.run();
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
