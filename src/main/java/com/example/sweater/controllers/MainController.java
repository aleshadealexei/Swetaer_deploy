package com.example.sweater.controllers;

import com.example.sweater.domain.Message;
import com.example.sweater.domain.Role;
import com.example.sweater.domain.User;
import com.example.sweater.repos.MessageRepo;
import com.example.sweater.repos.UserRepo;
import com.example.sweater.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class MainController {
    @Autowired
    private MessageRepo messageRepo;



    @GetMapping("/")
    public String GreetingMessage() {
        return "redirect:/main";
    }
    //перенаправляшка
    @GetMapping("/login")
    public String logged() {
        if (
                SecurityContextHolder.getContext().getAuthentication() != null &&
                        SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                        !(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)

        ) {

            return "redirect:/main";
        } else {
            return "login";
        }
    }

    @Autowired
    private UserRepo userRepo;
    //заход на главную
    @GetMapping("/main")
    public String main(Model model, @AuthenticationPrincipal User user,
                       @PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC, size = 3) Pageable pageable) {

        User us1 = userRepo.findByUsername(user.getUsername());
        Page<Message> messagePage = messageRepo.findAll(pageable);
        Iterable<Message> messages = messageRepo.findAllByOrderByIdDesc();
        model.addAttribute("page", messagePage);
        model.addAttribute("user", us1);
        model.addAttribute("admin", user.getRoles().contains(Role.ADMIN));
        model.addAttribute("messages", messages);
        model.addAttribute("logined", true);
        return "main";
    }
    //добавить сообщение
    @PostMapping("/main")
    public String add(@AuthenticationPrincipal User user,
                      @Valid Message message,
                      BindingResult bindingResult,
                      Model model,
                      @RequestParam(name="file", required = false, defaultValue = "null") MultipartFile file
                      ) throws IOException {


        message.setAuthor(user);
        if (bindingResult.hasErrors()) {
            Map<String, String> errorsMap = ControllerUtils.getErrors(bindingResult);
            model.mergeAttributes(errorsMap);
            //return main(model, user,);
        } else {
            FileService.uploadAndSaveFile(message, file);
            messageRepo.save(message);
        }
        System.out.println(message.getText());




        return "redirect:/main";
    }




    //поиск сообщений
    @PostMapping("/main/find")
    public String find(Model model, @RequestParam String filter,
                       @AuthenticationPrincipal User user,
                       @PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC, size = 3) Pageable pageable) {


        if (messageRepo.findByTag(filter) != null) {
            User us1 = userRepo.findByUsername(user.getUsername());
            Page<Message> messagePage = messageRepo.findByTag(filter, pageable);
            Iterable<Message> messages =  messageRepo.findByTag(filter, pageable);
            model.addAttribute("page", messagePage);
            model.addAttribute("user", us1);
            model.addAttribute("admin", user.getRoles().contains(Role.ADMIN));
            model.addAttribute("messages", messages);
            return "main";
        } else {
            return "redirect:/main";
        }
    }


}
