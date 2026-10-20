package fr.jmproduction.pocketdoor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Carte galactique du Tertarium : interface de navigation sans recentrage automatique. */
public final class CartostellaireScreen extends Screen {
    private static final StarSystem[] SYSTEMS = {
        new StarSystem("Nébuleuse d'Ambre", "Veines solaires et portes anciennes", 0xE7A86B, -148, -62, new CelestialBody[]{
            b("Auralis", "Un monde sonore où les portes vibrent sous les vents.", "9 840 km", "0,82 atm", "Azote / Oxygène", 0.78F, 0xC77DFF, 0.0F), b("Nymbrune", "Des brumes bleues gardent les archives des premiers voyageurs.", "12 420 km", "1,06 atm", "Azote / Vapeur d'eau", 0.64F, 0x62D4C8, 1.6F), b("Kéraphos", "Un désert de cuivre constellé de canyons lumineux.", "7 210 km", "0,44 atm", "Argon / Poussière", 0.39F, 0xD98962, 3.0F), b("Sélénor", "Lune pâle aux marées lentes et aux jardins minéraux.", "3 980 km", "0,12 atm", "Aucune", 0.27F, 0xA9B7D4, 4.2F)}),
        new StarSystem("Bras de Verre", "Un chapelet d'étoiles froides", 0x93D4E8, -28, -108, new CelestialBody[]{
            b("Veyl", "Une planète silencieuse au-delà de tout signal connu.", "14 080 km", "1,34 atm", "Azote / Méthane", 0.83F, 0xE28B63, .5F), b("Orrery IX", "Un monde instable, couvert de fragments orbitaux.", "10 600 km", "0,67 atm", "Oxygène / Néon", 0.51F, 0x65749A, 1.9F), b("Miroir-7", "Ses mers reflètent des constellations qui n'existent pas.", "16 300 km", "2,10 atm", "Hydrogène / Hélium", 0.46F, 0x7EB9D8, 3.4F), b("Tesselle", "Des plateaux géométriques émergent d'un océan noir.", "6 740 km", "0,31 atm", "Azote / Cristaux", 0.72F, 0xD6A9E8, 4.7F)}),
        new StarSystem("Couronne de Silex", "Étoiles vives, mondes minéraux", 0xE8D57D, 122, -76, new CelestialBody[]{
            b("Rhéa-Vert", "Une canopée phosphorescente recouvre ses continents.", "11 900 km", "1,18 atm", "Oxygène / Azote", 0.91F, 0x71D9B2, .2F), b("Cendreline", "Des volcans lents sculptent un horizon rouge sombre.", "8 450 km", "3,42 atm", "Dioxyde de carbone", 0.22F, 0xC96D58, 1.5F), b("Luméa", "Des plaines de sel chantent sous deux lunes.", "13 760 km", "0,96 atm", "Azote / Oxygène", 0.68F, 0xF2B84B, 3.1F), b("Périgée", "Petite planète captive, glacée mais riche en Tertarium.", "5 230 km", "0,08 atm", "Méthane toxique", 0.88F, 0x8E9FE5, 4.5F)}),
        new StarSystem("Rift des Lucioles", "Une faille constellée de poussières", 0xD69BE6, -102, 76, new CelestialBody[]{
            b("Solivane", "Des cités suspendues suivent les courants de lumière.", "15 440 km", "1,01 atm", "Azote / Oxygène", 0.76F, 0xD5A2F1, .7F), b("Mauve-Delta", "Un marais violet abrite des formes de vie translucides.", "9 180 km", "1,72 atm", "Vapeur d'eau / Soufre", 0.57F, 0xA86FCC, 2.0F), b("Nacre", "Ses falaises blanches brillent comme des ossements.", "12 080 km", "0,73 atm", "Oxygène / Azote", 0.34F, 0xE6D7C8, 3.3F), b("Fulgur", "Un monde d'orages permanents et de nuits électriques.", "18 220 km", "4,80 atm", "Hydrogène / Méthane", 0.18F, 0x6B8FE8, 4.8F)}),
        new StarSystem("Anneau des Veilleurs", "Le dernier relais avant le vide", 0xA6C7B0, 96, 72, new CelestialBody[]{
            b("Tertialis", "Un sanctuaire de pierre où la résonance reste intacte.", "10 010 km", "0,99 atm", "Azote / Oxygène", 0.97F, 0xC9A86A, .1F), b("Orphée", "Ses dunes noires dessinent des cartes vers des portes closes.", "7 880 km", "0,22 atm", "Argon / Poussière", 0.63F, 0x7686A8, 1.7F), b("Iskandre", "Une mer de nuages abrite des îles flottantes.", "17 600 km", "2,64 atm", "Hélium / Vapeur d'eau", 0.41F, 0x6FC9C0, 3.0F), b("Vesper", "Le crépuscule y dure des siècles, sans jamais s'éteindre.", "13 240 km", "1,43 atm", "Méthane toxique", 0.86F, 0xB477CF, 4.4F)})
    };
    private static final int PANEL_W = 128;
    private static final int PANEL_GAP = 12;
    private static final StarPoint[] GALAXY_STARS = createGalaxyStars();
    private float zoom = 0.82F, panX, panY, dragX, dragY;
    private int selectedSystem = 0, selectedPlanet = 0;
    private boolean inSystem, dragging;
    private long frame;
    private boolean panelOpen = true, dataOpen = true, tertariumOpen = true;
    private float panelSlide, mapCenterX, mapCenterY;
    private int panelScroll;
    private boolean scrollbarDragging;
    
    private float scrollbarDragOffset;

    private static StarPoint[] createGalaxyStars() {
        Random random = new Random(0x73C0FFEE);
        // Grille déterministe de 20 x 15 : environ 3,5 fois moins d'étoiles que v12.
        final int columns = 20, rows = 15;
        final float cellWidth = 42.0F, cellHeight = 42.0F;
        final float jitter = 8.0F;
        StarPoint[] stars = new StarPoint[columns * rows];
        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                // Un point par cellule, avec ±8 px de jitter : écart minimal garanti de 26 px.
                float x = -410.0F + 21.0F + column * cellWidth + (random.nextFloat() * 2.0F - 1.0F) * jitter;
                float y = -320.0F + 21.0F + row * cellHeight + (random.nextFloat() * 2.0F - 1.0F) * jitter;
                // Fond étoilé volontairement monochrome : uniquement des pixels blancs/gris de 1 px.
                int[] colors={0xFFFFFFFF,0xFFF0F0F0,0xFFD0D0D0,0xFFAAAAAA};
                stars[index++]=new StarPoint(x,y,1,colors[random.nextInt(colors.length)],0.45F+random.nextFloat()*0.55F,random.nextFloat()*6.28F);
            }
        }
        return stars;
    }


    private static CelestialBody b(String n,String d,String s,String p,String a,float t,int c,float o){return new CelestialBody(n,d,s,p,a,t,c,o);}
    public CartostellaireScreen(){super(Component.literal("Cartostellaire"));}
    @Override public void tick(){ frame++; AnomalyController.onOverview(!inSystem, selectedSystem); }
    @Override public void render(PoseStack p,int mouseX,int mouseY,float partial){
        drawBackdrop(p); float target=panelOpen?1.0F:0.0F; panelSlide+=(target-panelSlide)*0.20F;
        drawHeader(p); float cx=width*0.5F, cy=Math.max(92,height*0.53F); mapCenterX=cx+panX; mapCenterY=cy+panY;
        p.pushPose(); p.translate(mapCenterX,mapCenterY,0); p.scale(zoom,zoom,1); if(inSystem) drawSystem(p); else drawGalaxy(p,partial); p.popPose();
        if(panelSlide>0.01F) drawPanel(p); drawControls(p);
        if (AnomalyController.isFluctuation() || AnomalyController.isDetected()) drawCenteredString(p,font,AnomalyController.message(),width/2,48,0xFFFF6B73);
        drawControls(p); drawCenteredString(p,font,Component.literal("MOLETTE : ZOOMER   •   GLISSER : EXPLORER   •   R : RECENTRER"),width/2,height-12,0xFF8290A8);
    }
    private void drawBackdrop(PoseStack p){
        // Fond uni sombre : seules les étoiles simples restent derrière la carte.
        fill(p,0,0,width,height,0xFF070A18);
        for(StarPoint s:GALAXY_STARS){
            float tw=s.brightness*(0.76F+0.24F*(float)Math.sin(frame*.035F+s.phase));
            int x=(int)((s.x+panX*.08F+width*.49F+frame*.004F*(s.y*.02F))%width);
            int y=(int)((s.y+panY*.08F+height*.48F)%height);
            if(x<0)x+=width;if(y<0)y+=height;
            int col=shade(s.color,tw);
            // Un carré strictement 1 px : aucun tiret ni rectangle allongé.
            fill(p,x,y,x+1,y+1,0xFF000000|col);
        }
    }
    private void drawHeader(PoseStack p){ drawString(p,font,Component.literal("CARTOSTELLAIRE"),18,14,0xFFC9A0FF); drawString(p,font,Component.literal(inSystem?"SYSTÈME // "+SYSTEMS[selectedSystem].name.toUpperCase():String.format("VUE GALACTIQUE // SYSTÈME %02d",SYSTEMS.length)),19,28,0xFF8290A8); fill(p,18,39,Math.min(width-18,310),40,0xFF6B4A94); }
    private void drawGalaxy(PoseStack p,float partial){
        if (AnomalyController.isDetected()) {
            float q=AnomalyController.markerProgress();
            float eased=q*q*(3.0F-2.0F*q);
            StarSystem target = SYSTEMS[AnomalyController.targetSystem()];
            float targetX=target.x, targetY=target.y;
            // Two readable legs: approach from outside to the chosen system, then
            // travel to a stable point for the chosen planet. User zoom/pan is untouched.
            float mx, my;
            if (eased < 0.5F) {
                float leg=eased*2.0F;
                mx=300.0F+(targetX-300.0F)*leg;
                my=-220.0F+(targetY+220.0F)*leg;
            } else {
                float leg=(eased-0.5F)*2.0F;
                CelestialBody body=target.bodies[AnomalyController.targetPlanet()];
                double angle=body.orbit+AnomalyController.targetPlanet()*2.0D*Math.PI/4.0D;
                float planetX=targetX+(float)(Math.cos(angle)*(58+AnomalyController.targetPlanet()*42));
                float planetY=targetY+(float)(Math.sin(angle)*(29+AnomalyController.targetPlanet()*18));
                mx=targetX+(planetX-targetX)*leg;
                my=targetY+(planetY-targetY)*leg;
            }
            fill(p,(int)mx-6,(int)my-6,(int)mx+7,(int)my+7,0xFFFF3030);
        }
        // La spirale est dessinée avant les marqueurs afin de rester derrière eux.
        drawGalaxySpiral(p);
        for(int i=0;i<SYSTEMS.length;i++){StarSystem s=SYSTEMS[i]; double a=frame*.0007+i*.18; int x=(int)(s.x*Math.cos(a)-s.y*Math.sin(a)),y=(int)(s.x*Math.sin(a)+s.y*Math.cos(a)); s.screenX=mapCenterX+x*zoom; s.screenY=mapCenterY+y*zoom; halo(p,x,y,13,s.color); star(p,x,y,8,s.color); drawCenteredString(p,font,Component.literal(s.name),x,y+17,0xFFDDE3F2); }
    }

    private void drawGalaxySpiral(PoseStack p){
        float pulse=0.78F+0.22F*(float)Math.sin(frame*0.045D);
        for(int arm=0;arm<5;arm++){
            double armOffset=arm*2.0D*Math.PI/5.0D;
            for(int radius=12;radius<=320;radius+=7){
                double angle=radius*0.020D+armOffset;
                int x=(int)(Math.cos(angle)*radius);
                int y=(int)(Math.sin(angle)*radius*0.52D);
                float core=(radius<72)?1.0F:(1.0F-(radius-72)/420.0F);
                int alpha=(int)(110.0F+85.0F*pulse*core);
                int size=(radius<80)?2:1;
                int color=(alpha<<24)|0x00FFFFFF;
                fill(p,x-size,y-size,x+size+1,y+size+1,color);
            }
        }
        fill(p,-3,-3,4,4,0xE8FFFFFF);
    }

    private void drawSystem(PoseStack p){
        for(int r=0;r<4;r++) orbit(p,58+r*42,29+r*18,r==selectedPlanet); halo(p,0,0,22,0xFFFFD27D); star(p,0,0,11,0xFFFFD27D);
        CelestialBody[] bs=SYSTEMS[selectedSystem].bodies; for(int i=0;i<bs.length;i++){CelestialBody b=bs[i]; double a=b.orbit+(i*2.0D*Math.PI/4.0D)+frame*(.002+i*.00035); int x=(int)(Math.cos(a)*(58+i*42)),y=(int)(Math.sin(a)*(29+i*18)); b.screenX=mapCenterX+x*zoom;b.screenY=mapCenterY+y*zoom; halo(p,x,y,10,b.color);cube(p,x,y,7+i,b.color); if(i==selectedPlanet){int k=13+i; fill(p,x-k,y-k,x-k+4,y-k+1,0xFFE8D7FF);fill(p,x+k-4,y-k,x+k,y-k+1,0xFFE8D7FF);fill(p,x-k,y+k-1,x-k+4,y+k,0xFFE8D7FF);fill(p,x+k-4,y+k-1,x+k,y+k,0xFFE8D7FF); drawCenteredString(p,font,Component.literal(b.name),x,y+18+i,0xFFF4E9FF);} else { /* labels intentionally quiet */ } }
    }
    private void orbit(PoseStack p,int rx,int ry,boolean selected){
        float pulse=0.78F+0.22F*(float)Math.sin(frame*.055D+(double)rx*.03D);
        int alpha=(int)((selected?224.0F:166.0F)*pulse);
        int color=selected?0x00AEEBFF:0x009D83D6;
        for(int a=0;a<360;a+=7){
            double t=Math.toRadians(a);
            int x=(int)(Math.cos(t)*rx),y=(int)(Math.sin(t)*ry);
            fill(p,x-1,y-1,x+2,y+2,(alpha<<24)|color);
        }
    }
    private void halo(PoseStack p,int x,int y,int radius,int color){for(int i=radius;i>radius-8&&i>1;i-=2)fill(p,x-i,y-i,x+i,y+i,((18+(radius-i)*8)<<24)|shade(color,.55F));}
    private void star(PoseStack p,int x,int y,int s,int color){int d=Math.max(2,s/2);fill(p,x-s,y-s,x+s,y+s,0xFF000000|color);fill(p,x-s,y-s,x+s,y-s+d,0xFF000000|shade(color,1.3F));fill(p,x+s-d,y-s+d,x+s,y+s,0xFF000000|shade(color,.52F));}
    private void cube(PoseStack p,int x,int y,int s,int color){int d=Math.max(3,s/2);fill(p,x-s,y-s,x+s,y+s,0xFF000000|color);fill(p,x-s,y-s,x+s,y-s+d,0xFF000000|shade(color,1.28F));fill(p,x+s-d,y-s+d,x+s,y+s,0xFF000000|shade(color,.52F));}
    private int contentTop(CelestialBody w, int left, int right) {
        int descriptionLines = Math.max(1, Math.min(3, font.split(Component.literal(w.description), Math.max(1, right - left - 15)).size()));
        return 48 + 8 + 15 + 17 + descriptionLines * 11 + 5;
    }
    private int viewportBottom() { return Math.max(48 + 30, height - 30); }
    private int contentHeight(CelestialBody w, int left, int right) {
        int h = 20; // DONNEES header
        if (dataOpen) {
            String[][] rows={{"Taille",w.size},{"Pression",w.pressure},{"Atmosphère",w.atmosphere},{"Gravité",gravity(w)},{"Température",temperature(w)}};
            for (String[] row : rows) h += dataRowHeight(row[1], Math.max(1, right-left-12));
            h += 6;
        }
        h += 20; // TERTARIUM header
        if (tertariumOpen) h += 27;
        return h;
    }
    private int dataRowHeight(String value, int maxWidth) {
        return Math.max(11, font.split(Component.literal(value), Math.max(1, maxWidth)).size() * 11) + 5;
    }
    private int maxPanelScroll(CelestialBody w, int left, int right) {
        int top = contentTop(w, left, right);
        return Math.max(0, contentHeight(w, left, right) - (viewportBottom() - top));
    }
    private void clampPanelScroll(CelestialBody w, int left, int right) {
        panelScroll = Mth.clamp(panelScroll, 0, maxPanelScroll(w, left, right));
    }
    private void drawPanel(PoseStack p){
        int right=width-12;
        int left=right-(int)(PANEL_W*panelSlide);
        int bottom=viewportBottom();
        fill(p,left,48,right,bottom,0xF0141B30);
        fill(p,left,48,left+2,bottom,0xFFC9A0FF);
        if(!inSystem){
            int x=left+8,y=56;
            drawString(p,font,Component.literal("ARCHIPEL"),x,y,0xFF8290A8);
            drawString(p,font,Component.literal(SYSTEMS[selectedSystem].name),x,y+15,0xFFF0D8FF);
            drawWrap(p,SYSTEMS[selectedSystem].description,x,y+32,right-7,3,0xFFD4D9E5);
            drawString(p,font,Component.literal("5 systèmes • 20 mondes"),x,y+72,0xFF9CB0CA);
            return;
        }
        CelestialBody w=SYSTEMS[selectedSystem].bodies[selectedPlanet];
        int x=left+8, headerY=56;
        drawString(p,font,Component.literal("PLANÈTE"),x,headerY,0xFF8290A8);
        drawString(p,font,Component.literal(w.name),x,headerY+15,0xFFF0D8FF);
        int descLines=Math.max(1,Math.min(3,font.split(Component.literal(w.description),Math.max(1,right-x-7)).size()));
        drawWrap(p,w.description,x,headerY+32,right-7,3,0xFFD4D9E5);
        int viewportTop=contentTop(w,left,right);
        clampPanelScroll(w,left,right);
        int contentY=viewportTop-panelScroll;
        int scissorX=Math.max(0,left), scissorY=Math.max(0, height-(bottom)), scissorW=Math.max(1,right-left), scissorH=Math.max(1,bottom-viewportTop);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor((int)(scissorX*minecraft.getWindow().getGuiScale()), (int)(scissorY*minecraft.getWindow().getGuiScale()), (int)(scissorW*minecraft.getWindow().getGuiScale()), (int)(scissorH*minecraft.getWindow().getGuiScale()));
        int yy=contentY;
        fill(p,x,yy,right-7,yy+17,0xFF273149);
        drawString(p,font,Component.literal((dataOpen?"−":"+")+" DONNÉES"),x+4,yy+4,0xFFE5C7FF);
        yy+=20;
        if(dataOpen){
            String[][] rows={{"Taille",w.size},{"Pression",w.pressure},{"Atmosphère",w.atmosphere},{"Gravité",gravity(w)},{"Température",temperature(w)}};
            for(String[] row:rows){
                drawString(p,font,Component.literal(row[0]),x+2,yy,0xFF8FA0B8);
                int labelWidth=font.width(row[0]);
                int valueX=x+2+labelWidth+8;
                int available=right-7-valueX;
                int lines=font.split(Component.literal(row[1]),Math.max(1,available)).size();
                if(lines>1){ valueX=x+2; yy+=11; available=right-7-valueX; }
                drawWrap(p,row[1],valueX,yy,right-7,Math.max(1,lines),0xFFD4D9E5);
                yy+=dataRowHeight(row[1],Math.max(1,available));
            }
            yy+=1;
        }
        fill(p,x,yy,right-7,yy+17,0xFF273149);
        drawString(p,font,Component.literal((tertariumOpen?"−":"+")+" TERTARIUM"),x+4,yy+4,0xFFE5C7FF);
        yy+=20;
        if(tertariumOpen){
            int pct=Math.round(w.tertarium*100);
            drawString(p,font,Component.literal(pct+" %"),x+4,yy,0xFF71D9B2);
            int bar=yy+13;
            fill(p,x+4,bar,right-7,bar+4,0xFF303A54);
            fill(p,x+4,bar,x+4+(int)((right-x-11)*w.tertarium),bar+4,0xFF71D9B2);
        }
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
        int maxScroll=maxPanelScroll(w,left,right);
        if(maxScroll>0){
            int trackTop=viewportTop, trackBottom=bottom, trackH=trackBottom-trackTop;
            int thumbH=Math.max(16,(int)((float)(trackH*trackH)/(trackH+maxScroll)));
            int thumbY=trackTop+(int)((float)(trackH-thumbH)*panelScroll/maxScroll);
            fill(p,right-5,trackTop,right-2,trackBottom,0xFF273149);
            fill(p,right-5,thumbY,right-2,thumbY+thumbH,0xFFC9A0FF);
        }
    }
    private int drawWrapHeight(String text,int maxWidth,int maxLines){return 11*Math.min(maxLines,font.split(Component.literal(text),Math.max(1,maxWidth)).size());}
    private int drawWrap(PoseStack p,String text,int x,int y,int right,int max,int color){List<net.minecraft.util.FormattedCharSequence> lines=font.split(Component.literal(text),Math.max(1,right-x));int n=Math.min(max,lines.size());for(int i=0;i<n;i++)drawString(p,font,lines.get(i),x,y+i*11,color);return y+n*11;}
    private void drawControls(PoseStack p){
        int x=width-38;
        fill(p,x,14,width-14,38,0xFF273149);
        drawCenteredString(p,font,Component.literal("×"),x+12,19,0xFFE5C7FF);
        if(inSystem){fill(p,18,45,94,63,0xFF273149);drawString(p,font,Component.literal("← GALAXIE"),25,50,0xFFE5C7FF);}
        int a=width-12-(int)(PANEL_W*panelSlide)-27;
        fill(p,a,14,a+22,38,0xFF273149);
        drawCenteredString(p,font,Component.literal(panelOpen?"›":"‹"),a+11,19,0xFFE5C7FF);
    }
    private boolean insidePanel(double x,double y){
        int right=width-12,left=right-(int)(PANEL_W*panelSlide);
        return panelSlide>.08F&&x>=left&&x<=right&&y>=48&&y<=viewportBottom();
    }
    private boolean onScrollbar(double x,double y){
        if(!inSystem)return false;
        int right=width-12,left=right-(int)(PANEL_W*panelSlide),bottom=viewportBottom();
        CelestialBody w=SYSTEMS[selectedSystem].bodies[selectedPlanet];
        int top=contentTop(w,left,right), max=maxPanelScroll(w,left,right);
        if(max<=0||x<right-8||x>right||y<top||y>bottom)return false;
        int track=bottom-top, thumb=Math.max(16,(int)((float)(track*track)/(track+max)));
        int thumbY=top+(int)((float)(track-thumb)*panelScroll/max);
        if(y<=thumbY+thumb){scrollbarDragOffset=(float)y-thumbY;return true;}
        panelScroll=(int)((y-top-thumb*.5F)*max/Math.max(1,track-thumb));clampPanelScroll(w,left,right);return true;
    }
    private void resetView(){panX=panY=0;zoom=inSystem?.86F:.68F;panelScroll=0;scrollbarDragging=false;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(x>=width-44&&y<=44){onClose();return true;}
        if(inSystem&&x>=16&&x<=100&&y>=42&&y<=70){inSystem=false;resetView();return true;}
        int toggle=width-12-(int)(PANEL_W*panelSlide)-30;
        if(x>=toggle&&x<=width-10&&y<=44){panelOpen=!panelOpen;return true;}
        if(onScrollbar(x,y)){scrollbarDragging=true;return true;}
        if(insidePanel(x,y)){
            if(inSystem){
                CelestialBody w=SYSTEMS[selectedSystem].bodies[selectedPlanet];
                int right=width-12,left=right-(int)(PANEL_W*panelSlide), yy=contentTop(w,left,right)-panelScroll;
                if(y>=yy&&y<=yy+19){dataOpen=!dataOpen;clampPanelScroll(w,left,right);return true;}
                yy+=20;
                if(dataOpen){String[][] rows={{"Taille",w.size},{"Pression",w.pressure},{"Atmosphère",w.atmosphere},{"Gravité",gravity(w)},{"Température",temperature(w)}};for(String[] row:rows)yy+=dataRowHeight(row[1],Math.max(1,right-left-12));yy+=1;}
                if(y>=yy&&y<=yy+19){tertariumOpen=!tertariumOpen;clampPanelScroll(w,left,right);return true;}
            }
            return true;
        }
        float best=Float.MAX_VALUE;int hit=-1;
        if(inSystem){CelestialBody[] bs=SYSTEMS[selectedSystem].bodies;for(int i=0;i<bs.length;i++){float d=(float)Math.hypot((float)(x-bs[i].screenX),(float)(y-bs[i].screenY));float r=9+i;if(d<=r&&d<best){best=d;hit=i;}}if(hit>=0){selectedPlanet=hit;panelOpen=true;resetView();return true;}}
        else{for(int i=0;i<SYSTEMS.length;i++){float d=(float)Math.hypot((float)(x-SYSTEMS[i].screenX),(float)(y-SYSTEMS[i].screenY));if(d<=13&&d<best){best=d;hit=i;}}if(hit>=0){selectedSystem=hit;inSystem=true;selectedPlanet=0;panelOpen=true;resetView();return true;}}
        if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT||button==GLFW.GLFW_MOUSE_BUTTON_MIDDLE){dragging=true;dragX=(float)x-panX;dragY=(float)y-panY;return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(scrollbarDragging&&inSystem){int right=width-12,left=right-(int)(PANEL_W*panelSlide),bottom=viewportBottom();CelestialBody w=SYSTEMS[selectedSystem].bodies[selectedPlanet];int top=contentTop(w,left,right),max=maxPanelScroll(w,left,right),track=bottom-top,thumb=Math.max(16,(int)((float)(track*track)/(track+max)));panelScroll=(int)(((float)y-scrollbarDragOffset-top)*max/Math.max(1,track-thumb));clampPanelScroll(w,left,right);return true;}
        if(insidePanel(x,y)){return true;}if(!dragging)return true;panX=(float)x-dragX;panY=(float)y-dragY;float lim=120F;panX=Mth.clamp(panX,-lim,lim);panY=Mth.clamp(panY,-lim,lim);return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){scrollbarDragging=false;dragging=false;return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double delta){
        if(insidePanel(x,y)){if(inSystem){int right=width-12,left=right-(int)(PANEL_W*panelSlide);CelestialBody w=SYSTEMS[selectedSystem].bodies[selectedPlanet];panelScroll-=((int)delta)*11;clampPanelScroll(w,left,right);}return true;}
        zoom=Mth.clamp(zoom+(float)delta*.08F,.48F,1.35F);return true;
    }
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}if(key==GLFW.GLFW_KEY_BACKSPACE&&inSystem){inSystem=false;resetView();return true;}if(key==GLFW.GLFW_KEY_R){resetView();return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){ AnomalyController.requestReturn(); CartostellaireClient.beginReturn(); super.onClose();}
    private static String gravity(CelestialBody w){return w.tertarium>.75F?"0,9 g":"0,6 g";} private static String temperature(CelestialBody w){return w.pressure.startsWith("0,")?"-78 °C":"+18 °C";} private static int shade(int rgb,float f){int r=Mth.clamp((int)(((rgb>>16)&255)*f),0,255),g=Mth.clamp((int)(((rgb>>8)&255)*f),0,255),b=Mth.clamp((int)((rgb&255)*f),0,255);return(r<<16)|(g<<8)|b;}
    private static final class StarPoint{final float x,y,brightness,phase;final int size,color;StarPoint(float a,float b,int s,int c,float br,float ph){x=a;y=b;size=s;color=c;brightness=br;phase=ph;}}
    private static final class StarSystem{final String name,description;final int color,x,y;final CelestialBody[] bodies;float screenX,screenY;StarSystem(String n,String d,int c,int px,int py,CelestialBody[] b){name=n;description=d;color=c;x=px;y=py;bodies=b;}}
    private static final class CelestialBody{final String name,description,size,pressure,atmosphere;final float tertarium,orbit;final int color;float screenX,screenY;CelestialBody(String n,String d,String s,String p,String a,float t,int c,float o){name=n;description=d;size=s;pressure=p;atmosphere=a;tertarium=t;color=c;orbit=o;}}
}
