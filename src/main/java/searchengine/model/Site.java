package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "site")
@Data
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private int id;
    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;
    @Column(name = "status_time")
    private Timestamp statusTime;
    @Column(name = "last_error")
    private String lastError = null;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String url;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;
    @OneToMany
    @JoinColumn(name = "site_id")
    List<Page> pages;
    @OneToMany
    @JoinColumn(name = "site_id")
    List<Lemma> lemmas;


}
