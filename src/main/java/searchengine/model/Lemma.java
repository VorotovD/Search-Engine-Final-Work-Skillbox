package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "lemma",uniqueConstraints = @UniqueConstraint(columnNames = "lemma"))
@NoArgsConstructor
@Setter
@Getter
public class Lemma {
    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotNull
    private int frequency;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;
    @NotNull
    @Column(name = "site_id")
    private int siteId;
    @ManyToOne
    @JoinColumn(name = "site_id",insertable = false,updatable = false,nullable = false)
    private SitePage sitePage;

}
