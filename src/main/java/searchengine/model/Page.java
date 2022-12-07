package searchengine.model;

import com.sun.istack.NotNull;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.List;

@Entity
@Table(name = "page",indexes = {@Index(name = "path_index",columnList = "path")})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private int id;
    @NotNull
    @Column(name = "site_id")
    private int siteId;
    @NotNull
    private String path;
    @NotNull
    private int code;
    @NotNull
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
    @ManyToOne
    @JoinColumn(name = "site_id",nullable = false,insertable = false,updatable = false)
    private Site site;
}
