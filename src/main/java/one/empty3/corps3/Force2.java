package one.empty3.corps3;

import one.empty3.library.Point3D;
import one.empty3.library.core.physics.Bille;
import one.empty3.library.core.physics.Force;

import java.util.ArrayList;
import java.util.List;

public class Force2 extends Force {

    private double t;
    private double softening = 1.0;

    public Force2(double time) {
        super();
        setFusion(false);
        this.t = time;
        amortissement = 0.0;
        setG(1.0);
    }

    @Override
    public Point3D attractionRepulsion(Bille other, Bille p) {
        Point3D moins = other.position.moins(p.position);
        double distSq = moins.norme() * moins.norme();
        double denom = Math.pow(distSq + softening * softening, 1.5);
        if (denom == 0.0) {
            return Point3D.O0;
        }

        return moins.mult(getG()*(other.masse * p.masse) / denom);
    }
    @Override
    public void calculer() {
        List<Bille> courantActuel = getCourant();
        ArrayList<Bille> courant2 = new ArrayList<>();
        for (int i = 0; i < courantActuel.size(); i++) {
            Bille courante = courantActuel.get(i);
            Bille set = copy(courante);
            if (courante.masse <= 0.0) {
                set.vitesse = courante.vitesse;
                set.position = courante.position.plus(set.vitesse.mult(getDt()));
                courant2.add(set);
                continue;
            }
            Point3D force = force(i);
            set.vitesse = courante.vitesse.plus((force.mult(1.0 / courante.masse)).mult(getDt()));
            set.position = courante.position.plus(set.vitesse.mult(getDt()));
            courant2.add(set);
        }
        setCourant(courant2);
        t = t + getDt();
    }

    @Override
    public Point3D force(int ind) {
        Point3D f = Point3D.O0;

        for (int i = 0; i < getCourant().size(); i++) {
            if (i == ind)
                continue;
            f = f.plus(attractionRepulsion(getCourant().get(i), getCourant().get(ind)));
        }
        //f = f.plus(frottement(getCourant().get(ind)));

        return f;
    }

    private Bille copy(Bille bille) {
        Bille b = new Bille();
        b.masse = bille.masse;
        b.position = new Point3D(bille.position);
        b.vitesse = new Point3D(bille.vitesse);
        b.color = bille.color;

        return b;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public double getSoftening() {
        return softening;
    }

    public void setSoftening(double softening) {
        this.softening = softening;
    }
}
