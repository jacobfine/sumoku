package com.example.table;

import java.util.Arrays;
import java.util.Random;

public final class GameDef {
    double genTime = 0.0;
    double tileTime = 0.0;
    double solveTime = 0.0;
    double colorTime = 0.0;//diagnostic information for runspeed
    int solutions=0;
    int guessNum=0;
    private int maxSolutions = 1;//sets the game to only have 1 solution
    long myseed;  // Seed used by most recent gen()

    // tiling data and color combined
    private final int[] colors = new int[81];
    final short[] values = new short[81];

    private final int[] bitFirst = new int[1<<9];//table of the first active bit in values up to 2^9-1
    private final int[] bitCounts = new int[1<<9];//table of number of active bits in values up to 2^9-1
    final class Rand extends Random {
        long getSeed () {
            long result=(long)(nextInt())&0xFFFFFFFFL;
            setSeed(result);
            return result;
        }
        Rand(long seed) {
            super(seed);
        }
    }
    private final Rand rand;//sets random seed
    //random seed option to be nonrandom seed needs to be added

    int genCalls;  // Diagnostic stats
    int tileCalls;

    private void initTables(){//initilize the bitCounts and bitFirst tables
        bitCounts[0]=0;
        bitFirst[0]='*'-'1';//bitFirst[0] is output as '*' after adding '1' for diagnostic purposes
        for(int i=0; i<9; i++){
            for(int j=1<<i; j<1<<9; j+=2<<i)
                bitFirst[j]=i;//sets all values with i as rightmost 1 to i
            for(int j=1<<i; j<2<<i; j++)
                bitCounts[j]= bitCounts[j-(1<<i)]+1;
            //sets each value to be one more than the value without its left most 1
        }
    }

    GameDef() {//runs once at app startup
        rand = new Rand(System.currentTimeMillis());
        initTables();
        newGame();
    }

    final class generator{//produces answers for puzzle
        final int[] mask=new int[9];//for each collumn, which values have not been placed are indicating by active bits
        boolean gen() {//brute force creation of a puzzle layout
            genCalls++;//diagnostic
            Arrays.fill(mask,511);//511 is first 9 bits active, becuase no values have been placed
            Arrays.fill(values, (short)0);//values are not yet known
            for (int row=9; row-->0;){//loop through rows from 8 to 0
                int rMask=511;//row mask is open valuies in this row
                for (int work=511; work!=0;) {//work is open tiles in this row
                    int best=10;//to find best next item, actual worst value is 9
                    int who=0;//will store best item to work on
                    for (int col=9; col-->0;) {//loop through the collumns
                        if(((1<<col)&work)!=0){//if the row and collumn is open
                            int c= bitCounts[mask[col]&rMask];//size of overlap between that collumn and this row
                            if(c<best) {
                                best=c;
                                who=col;
                            }//smallest overlap becomes best
                        }
                    }//finds column with fewest remaining choices
                    int choice=mask[who]&rMask;
                    if (choice==0) {
                        return false;
                    }//if nothing fits, fail
                    for(int select=rand.nextInt()%best; select-->0;)
                        choice&=choice-1;//removes random number of rightmost 1's
                    choice&=-choice; //removes all but rightmost 1
                    values[row+9*who]= (short)choice;
                    rMask-= choice;
                    mask[who]-= choice;
                    work -= 1<<who;
                    //answer is placed and removed from row and column
                }
            }
            //table has been filled by naive guessing strategy
            return true;
        }
    }

    private void tile() {//creates the pattern of connections that form sums
        tileCalls++;//diagnostic
        Arrays.fill(colors, 0);
        tile(-1);
        for (int i=0; i<81; i++) colors[i]=colors[i]/(2-(colors[i]&1));
    }
    private boolean tile(int i) {//attempt to create tile pattern from square at position i
        do {
            if (++i>80) return true;//tiling complete
        } while (colors[i] != 0);//this tile is set to be the second in a group, we are done with it

        int direction=rand.nextBoolean()?-1:-9;//choose preffered connection direction by coinflip
        boolean join = rand.nextInt()%15==0;//choose whether this preffers to be part of a triple
        if (join) return tryjoin(i, direction)||tryjoin(i, direction^8)||trytile(i,direction)||trytile(i,direction^8);
        return trytile(i,direction)||trytile(i,direction^8)||tryjoin(i, direction)||tryjoin(i, direction^8);
        //attempts to tile every way with preferences accepted if they succeed
    }
    private boolean trytile(final int i, final int d) {//checks if connection works
        if (i-d>80||((d==-1)&&(i-d)%9==0)) return false; //fail if fall off board edge
        if (colors[i-d]!=0) return false;//fail if square is taken
        colors[i-d]=d;//connect
        if (tile(i)) return true;//attempt to complete tiling
        colors[i-d]=0;//undo connection if fail
        return false;
    }
    private boolean tryjoin(final int i, final int d) {//check if joining a double works
        if ( i+d>=0 && colors[i+d]==d ) {//check if the tile is part of a double in correct direction
            if ( (d==-1) && ((i%9)<2) ) return false;//fail if fall off board edge
            colors[i]=d*2;//connect
            if (tile(i)) return true;//attempt to complete tiling
            colors[i]=0;//undo connection if fail
        }
        return false;
    }
    private void colorIn(){ colorIn(1);}
    private boolean colorIn(int i) {//colors tiles to not match neighbors
        if (i>80) return true;
        if (colors[i]<0) {//case of not the root of a tile
            int c = getColor(i + colors[i]);//grab color of connected square
            if (i>9&&(c == getColor(i + (-10-colors[i]))))
                return false;//if it matches adjacent tile, fail
            return colorIn(i + 1);
        }
        int c=i>8?getColor(i-9):getColor(i-1);//store the color above unless above is off board, in which case store color to left
        for (int hue=c; (hue=(hue+1)%6)!=c; )//loop through 5 colors, starting after and not trying c
            if ((i%9==0)||hue!=getColor(i-1)) {//if there isn't a left neighbor or it isn't color hue
                 if (i<9||(hue!=colors[i-8])||(i%9==8)){
                    colors[i] = hue;//if color does not match diagonal up and to the right, use it
                    if (colorIn(i + 1)) return true;
                }
            }
        return false;
    }

    //backup of data for solver, smaller than backing up whole structure
    class backup {
        final short[] cell;
        final short[] line;  // Indexed by value{0..8} + 9 * (row_or_col{0..8} + 9 * is_col(0..1})
        final short[] sums;
        int ssize;

        backup() {
            cell = new short[81];
            line = new short[162];
            sums = new short[54*4];
            ssize=0;
        }
        backup(backup from)
        {
            cell = from.cell.clone();
            line = from.line.clone();
            ssize = from.ssize;
            sums = new short[ssize*4];
            System.arraycopy(from.sums,0,sums,0,ssize*4);
        }

        final void copy(backup b){
            ssize = b.ssize;
            System.arraycopy(b.cell,0,cell,0,81);
            System.arraycopy(b.line,0,line,0,162);
            System.arraycopy(b.sums,0,sums,0,ssize*4);
        }
    }

    private int[] sumMask=new int[]{~0, ~0x3, ~0x5, ~0xf, ~0x1b, 0x1c0, 0x188, 0x100,
            0x10, 0x1, 0x23, 0x7, ~0x1b0, 0x1f, ~0x140, ~0x180};//hexidecimal masks for legal values to make sums from 2 to 17

    private int[] triMask=new int[]{~0,~0,~0,  // 3,4,5 impossible
            ~0x7,          // 6 is 1,2,3
            ~0xb,          // 7 is 1,2,4
            ~0x1f,         // 8 is 1..5
            ~0x3f,         // 9 is 1..6
            ~0x7f,         // 10 is 1..7
            ~0xff,         // 11 is 1..8
            0,0,0,0,0,0,0, // 12..18 are anything
            ~0x1fe,        // 19 is 2..9
            ~0x1fc,        // 20 is 3..9
            ~0x1f8,        // 21 is 4..9
            ~0x1f0,        // 22 is 5..9
            ~0x1a0,        // 23 is 6,8,9
            ~0x1c0         // 24 is 7,8,9
    };

    final class Solver extends backup {//solver to prove solutions are unique
        int solutionsFound;
        final short[] Mcell = new short[81];//stores what changes made to a cell
        final short[] Mline = new short[162];//stores what changes each cell has made to each row and collumn
        final short[] cs = new short[81];//stores which sum each cell is part of
        final short[] cs2 = new short[81];//stores which sum each cell is part of
        final short[] cstack = new short[81];//stack of changes to cells to be processed
        int cstackp=-1;
        final short[] lstack = new short[162];//stack of changes to lines to be processed
        int lstackp=-1;
        Solver (){//base work that follows from puzzles with no guessing or complex deduction
            Arrays.fill(Mcell, (short)0);
            Arrays.fill(Mline, (short)0);
            Arrays.fill(cell, (short)511);
            Arrays.fill(line, (short)511);//prepare arrays for use
            Arrays.fill(cs2, (short)-1);

            for (int i=0; i<81; i++) {
                if (colors[i]>=0) {
                    addSum(i);//adds sums to sums array and makes simple deductions
                }//sums are retrieved from puzzle and added to list from the root cell
            }
            checkLines(9,1);
            checkLines(1,9);
            server();//process deductions from adding sums
        }

        void addSum(int position) {
            int direction=(colors[position+1]==-1)?-1:-9;//orientation of this tile, up down or right left
            int next=position-direction;//second cell in tile
            cs[position]=cs[next]=(short)(ssize*4);//store position in sums array of these cells in cs table
            int triplet=next-direction;//location of triplet if it exists
            if (triplet < 81 && colors[triplet]==direction) {// if third member exists
                cs[triplet] = (short)(ssize * 4);
            } else {
                triplet = -1;
            }
            addsum(position, next, triplet);
        }
        void addsum(int p1, int p2, int p3) {//add the sum and which tiles are part of that sum to sums array
            sums[ssize*4+1]=(short)p1;//sums array takes 4 elements per sum
            sums[ssize*4+2]=(short)p2;
            sums[ssize*4+3]=(short)p3;
            sums[ssize*4]=(short)(values[p1]+values[p2]);//store sum of first to tiles
            if (p3>=0) {//check if triplet
                sums[ssize*4] += values[p3];
                cellp(p1, triMask[sums[ssize*4]]);
                cellp(p2, triMask[sums[ssize*4]]);
                cellp(p3, triMask[sums[ssize*4]]);
            }
            else {
                cellp(p1, sumMask[sums[ssize*4]]);
                cellp(p2, sumMask[sums[ssize*4]]);
            }
            ++ssize;
        }

        void checkLines( int n1, int n2 ) {
            int[] cross = new int[3];
            int c;
            int tot;
            int special;
            for ( int s = n1*8; s>=0; s-=n1 ) { // loop through lines
                c = -1;
                tot = 36;
                special = -1;
                for ( int i = n2*8; i>=0; i-=n2 ) { // loop through elements
                    int x=s+i;
                    if (colors[x]+n2 == 0) { // pair or triple aligned with line
                        int group = values[x] + values[s+(i-=n2)];
                        if (colors[s+i]+n2 == 0) { // triple
                            group += values[s+(i-=n2)];
                        }
                        tot -= group;
                    } else {  // pair or triple orthogonal to line
                        if (++c<=2)
                            cross[c] = x;
                        if (colors[x] < 0) {  // misaligned item continues up or left
                            special = (special==-1) ? x : -2;
                        }
                    }
                }
                if (c==0) { // All but one element aligned with line
                    cellp(cross[0], 511-(1<<(tot)));  // So we know exact value of that element
                } else if (c<3 && c>0) {
                    cs2[cross[0]]=cs2[cross[1]]=(short)(ssize*4);//store position in sums array of these cells in cs table
                    if (c==2) cs2[cross[2]]=(short)(ssize*4);
                    addsum(cross[0],cross[1],(c==1)?-1:cross[2]);

                }
                if (special>=0) {
                    if (colors[special-n1]>=0)  // if special is not the end of a triple, this bit uses information provably deducable, but does cheat by looking up rather than deducing it
                        special -= n1;
                    cellp(special, 511-(1<<values[special]));  // So we know exact value of that element
                }
            }
        }
        boolean cellp(int c, int mask) {
            //remove mask from cell[c]
            if ((mask&=cell[c])==0)return true;//store overlap in mask, return if no overlap
            if ((cell[c]-=mask)==0) return false;//remove overlap, failure if no solutions remain
            if (Mcell[c]==0) cstack[++cstackp]=(short)c; //add cell to stack if it wasn't already
            Mcell[c]|=mask;//add change to process q
            return true;
        }
        boolean linep(int c, int mask) {//inverted usage, to avoid having inverted return logic (returns true on succes, but we only care if it fails)
            // c is value{0..8} + 9 * (row_or_col{0..8} + 9 * is_col(0..1})
            // mask is removed from possible orthogonal positions for that value in that line
            if ((mask&=line[c])==0)return true;//store overlap in mask, return if no overlap
            if ((line[c]-=mask)==0) return false;//remove overlap, failure if no solutions remain
            if (Mline[c]==0) lstack[++lstackp]=(short)c;//add line to change stack
            Mline[c]|=mask;//add line change to process q
            return true;
        }
        boolean lprop(int ln) {//line changes only matter when only 1 option for line
            int val=ln%9;//retrieve value hidden in index number
            int minor= bitFirst[line[ln]];//Only place the value can go
            int major=ln/9;  // this is either row or col+9
            int c,s,i;
            if (major>=9)  // major is col+9 // minor is row
            {
                s = major-9;     // first cell position in our line
                c = minor*9+s;   // special cell position (row*9+col)
                i = 9;
            } else {  // major is row // minor is col
                s = major*9;
                c = s+minor;
                i = 1;
            }
            if(bitCounts[line[ln]]==1) {// val can go in only one place
                if ( ! cellp( c, 511-(1<<val)) ) return false; //propagate change to the cell that has been singled out
            }
            int mask = Mline[ln];  // Places we previously thought val could be
            while (mask != 0) {
                int x = bitFirst[mask];
                mask -= 1<<x;
                x = x*i + s;
                if ( ! cellp( x, (1<<val)) ) return false; //propagate to rest of line
            }
            Mline[ln]=0;//clear line to-do list
            return true;
        }
        boolean cprop(int c)//inverted usage, to avoid having inverted return logic (returns true on succes, but we only care if it fails)
        {
            if ( ! cprop(c,cs[c]) ) return false;
            if ( cs2[c]>=0 && ! cprop(c,cs2[c]) ) return false;
            int col=c%9;
            int row=c/9;
            int mask = Mcell[c];
            while (mask != 0) {
                int v = bitFirst[mask];
                mask -= 1 << v;
                if ( ! linep(row*9+v, 1<<col) ) return false;
                if ( ! linep(col*9+v+81, 1<<row) ) return false;
            }
            Mcell[c]=0;//clear change log
            return true;
        }
        boolean cprop(int c, int s) {//cell change propagate
            int cnt = bitCounts[cell[c]];
            if(sums[s+3]==-1) {//if only 2 cells in sum
                int total=0;
                int changes = Mcell[c] & ((1<<(sums[s]+1))-1);
                while (changes!=0) {  // For each possibility being removed from cell c
                    int n = bitFirst[changes];
                    changes -= 1<<n;
                    total += 1<<(sums[s]-n);  // Accumulate possibilities to remove from cell of sum
                }
                int partner = sums[s+(sums[s+1]==c?2:1)];
                if((total&=511)!=0)//changes to one cell affect the other
                    if (!cellp(partner, total))//propogate to other cell in sum
                        return false;//fail if propogation fails
                if (cnt==2 && cell[c]==cell[partner]) {
                    int v1 = bitFirst[cell[c]];
                    int v2 = bitFirst[cell[c]-(1<<v1)];
                    int m, ln;
                    if ((c-partner)%9 != 0) {  // true for row
                        ln = c-(c%9); // 9 * shared row
                        m = 511 - (1<<(c%9)) - (1<<(partner%9));
                    } else {
                        ln = 9*(c%9)+81; // 9 * shared col + offset for col
                        m = 511 - (1<<(c/9)) - (1<<(partner/9));
                    }
                    if ( !linep(ln+v1, m) ) return false;
                    if ( !linep(ln+v2, m) ) return false;
                }
            }

            if (cnt==1) {//if 1 option left
                int col=c%9;
                int row=c/9;
                int val= bitFirst[cell[c]];

                //find row and column, fail if option can't go there
                if(!linep(row*9+val, 511-(1<<col))) return false;
                if(!linep(col*9+81+val, 511-(1<<row))) return false;
                if(sums[s+3]!=-1) {
                    sums[s]-=val;//reduce sum by value of this cell
                    if (sums[s]<1||sums[s]>15) return false;
                    if (sums[s+1]==c) sums[s+1]=sums[s+3];
                    else if (sums[s+2]==c) sums[s+2]=sums[s+3];
                    sums[s+3]=-1;//remove this cell
                    if ( ! cellp(sums[s+1], sumMask[sums[s]]) ) return false;
                    return cellp(sums[s + 2], sumMask[sums[s]]);
                }//reduce to 2 cell sum if it was 3 cell
            }

            return true;
        }
        boolean server() {//runs through stacks
            for(;;) {
                if (cstackp>=0) {//process cell stack
                    if (!cprop(cstack[cstackp--])) return false;
                }
                else if (lstackp>=0) {//process line stack
                    if (!lprop(lstack[lstackp--])) return false;
                }

                else return true;
            }//propagates up failure
        }

        int guess(){//attempt to solve the puzzle, returns number of solutions
            solutionsFound=0;
            guessNum=0;
            guess(-1);
            return solutionsFound;
        }
        void guess(int i){
            do {
                if (++i>80) {
                    ++solutionsFound;
                    return;
                }
            } while (bitCounts[cell[i]]==1);//skip cells with only 1 value possible


            backup b=new backup(this);//creates backup, to restore from bad guess
            for(int k=1; k<1<<9; k<<=1) {//loop through 9 values
                if ((k&cell[i])!=0) {//if cell can have that value
                    guessNum++;
                    cellp(i, cell[i]-k);//remove all but that value from cell
                    if(server()) { //if server succeeds, solution still possible
                        guess(i);
                        if (solutionsFound > maxSolutions)
                            return;//end when too many solutions found
                    }
                    this.copy(b);
                    Arrays.fill(Mcell, (short)0);
                    Arrays.fill(Mline, (short)0);
                    cstackp=-1;
                    lstackp=-1;//restore from backup
                }
            }
        }
    }//end of solver class

    void newGame(){newGame(0);}
    void newGame(int minGuesses) {
        rand.setSeed(System.currentTimeMillis());
        loadGame(-1L, minGuesses);
    }

    void loadGame(final long seed, int minGuesses) {  // But if seed==-1 acts as newGame instead
        genCalls = 0;  // diagnostic stats
        tileCalls = 0;
        genTime = 0.0;
        tileTime = 0.0;
        solveTime = 0.0;
        colorTime = 0.0;
        do {
            generator naive = new generator();
            if (seed==-1L) {
                maxSolutions=1;
                myseed = rand.getSeed();
            } else {
                maxSolutions=98;  // Solver stops at one more than max
                rand.setSeed(myseed=seed);
            }

            genTime -= System.nanoTime();//diagnostic
            while (!naive.gen()) {  //naive generator runs until a puzzle is made
                myseed = rand.getSeed();
            }
            for (int i = 0; i < 81; i++)
                values[i] = (short) bitFirst[values[i]];//set values to be in numerical form not bit masks
            genTime += System.nanoTime();

            tileTime -= System.nanoTime();
            tile();
            tileTime += System.nanoTime();

            solveTime -= System.nanoTime();
            Solver bruteForce = new Solver();
            solutions = bruteForce.guess();
            solveTime += System.nanoTime();
        } while (guessNum<minGuesses || solutions>1 && seed==-1L);

        colorTime -= System.nanoTime();
        colorIn();
        colorTime += System.nanoTime();
    }

    short[] stepSolver (int[] userAns) {
        Solver step = new Solver();
        for (int i=0; i<81; i++) {
            if (userAns[i]>=0) step.cellp(i, 511-(1<<userAns[i]));
        }
        step.server();
        short[] result = new short[81];
        for (int i=0; i<81; i++) {

            if (bitCounts[step.cell[i]]==1) {
                result[i] = (short) bitFirst[step.cell[i]];
            }

            else result[i]=-1;
        }
        return result;
    }

    int getSum(int n) {//returns sum of tile this cell belongs to
        if (colors[n]<0)
            return 0;
        int s = values[n]+2;
        if (colors[n+1]==-1) {
            s += values[n + 1];
            if (n < 79 && colors[n + 2] == -1)
                return s + values[n + 2] + 1;
        }
        else {
            s += values[n+ 9];
            if (n < 63 && colors[n + 18] == -9)
                return s + values[n + 18] + 1;
        }
        return s;
    }
    int getColor(int n) {//returns color numerical code for this cell
        for (;;) {
            int c = colors[n];
            if (c >= 0)
                return c;
            n += c;
        }
    }
}
