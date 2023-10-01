package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "site")
@NoArgsConstructor
@Setter
@Getter
public class SitePage {
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
    @OneToMany(orphanRemoval = true)
    @JoinColumn(name = "site_id")
    private List<Page> pages;
    @OneToMany(orphanRemoval = true)
    @JoinColumn(name = "site_id")
    private List<Lemma> lemmas;
}
