package com.betterreads.book;

import com.betterreads.userbooks.UserBooks;
import com.betterreads.userbooks.UserBooksPrimaryKey;
import com.betterreads.userbooks.UserBooksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class BookController {

    private final String COVER_IMAGE_ROOT = "http://covers.openlibrary.org/b/id/";

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserBooksRepository userBooksRepository;

    @GetMapping("/books/{id}")
    public String getBookById(@PathVariable String id, Model model, @AuthenticationPrincipal OAuth2User principal) {
        Optional<Book> optBook = bookRepository.findById(id);

        if(optBook.isPresent()) {

            Book book = optBook.get();
            String coverImageUrl = "/images/no-image.png";
            if (book.getCoverIds() != null && book.getCoverIds().size() > 0) {
                coverImageUrl = COVER_IMAGE_ROOT + book.getCoverIds().get(0) + "-L.jpg";
            }

            model.addAttribute("coverImage", coverImageUrl);
            model.addAttribute("book", book);

            if(principal !=null && principal.getAttribute("login") != null) {
                String userId = principal.getAttribute("login");
                model.addAttribute("loginId", userId);

                UserBooksPrimaryKey userBooksPrimaryKey = new UserBooksPrimaryKey();
                userBooksPrimaryKey.setBookId(book.getId());
                userBooksPrimaryKey.setUserId(userId);

                Optional<UserBooks> optUserBooks = userBooksRepository.findById(userBooksPrimaryKey);
                if(optUserBooks.isPresent()) {
                    model.addAttribute("userBooks", optUserBooks.get());
                } else {
                    model.addAttribute("userBooks", new UserBooks());
                }
            }

            return "book"; //return book.html with model
        }

        return "book-not-found";
    }
}
