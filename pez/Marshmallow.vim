set nocompatible
let s:cpo_save=&cpo
set cpo&vim
cmap <D-g> <D-g>
imap <D-g> <D-g>
cmap <D-f> <D-f>
imap <D-f> <D-f>
cmap <D-a> <D-a>
imap <D-a> <D-a>
cmap <D-v> +
cmap <D-c> 
cmap <D-z> <D-z>
imap <D-z> <D-z>
cmap <S-D-s> <D-s>
imap <S-D-s> <D-s>
cmap <D-s> <D-s>
imap <D-s> <D-s>
cmap <D-w> <D-w>
imap <D-w> <D-w>
cmap <D-o> <D-o>
imap <D-o> <D-o>
cmap <D-n> <D-n>
imap <D-n> <D-n>
map! <xHome> <Home>
map! <xEnd> <End>
map! <S-xF4> <S-F4>
map! <S-xF3> <S-F3>
map! <S-xF2> <S-F2>
map! <S-xF1> <S-F1>
map! <xF4> <F4>
map! <xF3> <F3>
map! <xF2> <F2>
map! <xF1> <F1>
nmap ,hx wbF<df>f<df>
nmap ,ht wbi<tt>ea</tt>bb
nmap ,hs wbi<strong>ea</strong>bb
nmap ,hu wbi<u>ea</i>bb
nmap ,hi wbi<i>ea</i>bb
nmap ,he wbi<em>ea</em>bb
nmap ,hb wbi<b>ea</b>bb
nmap ,h6 _i<h6>A</h6>
nmap ,h5 _i<h5>A</h5>
nmap ,h4 _i<h4>A</h4>
nmap ,h3 _i<h3>A</h3>
nmap ,h2 _i<h2>A</h2>
nmap ,h1 _i<h1>A</h1>
nmap ,mh wbgueyei<ea></pa>bba
vmap <D-x> "+x
vmap <D-c> "+y
vmap <D-v> "-d"*P
nmap <D-v> "+gP
omap <D-g> <D-g>
vmap <D-g> <D-g>
nmap <D-g> n
omap <D-f> <D-f>
vmap <D-f> <D-f>
nmap <D-f> /
omap <D-a> <D-a>
vmap <D-a> <D-a>
nmap <D-a> :if &slm != ""|exe ":norm gggHG"| else|exe ":norm ggVG"|endif
omap <D-z> <D-z>
vmap <D-z> <D-z>gv
nmap <D-z> u
omap <S-D-s> <D-s>
vmap <S-D-s> <D-s>gv
nmap <S-D-s> :confirm saveas =getcwd()/
omap <D-s> <D-s>
vmap <D-s> <D-s>gv
nmap <D-s> :if expand("%") == ""|browse confirm w| else|confirm w|endif
omap <D-w> <D-w>
vmap <D-w> <D-w>gv
nmap <D-w> :if winheight(2) < 0 |   confirm enew | else |   confirm close | endif
omap <D-o> <D-o>
vmap <D-o> <D-o>gv
nmap <D-o> :browse confirm e
omap <D-n> <D-n>
vmap <D-n> <D-n>gv
nmap <D-n> :confirm enew
map Q gq
vnoremap p :let current_reg = @"gvdi=current_reg
map <F9> :if exists("syntax_on") | syntax off | else | syntax on | endif 
map <F1> previous
map <F4> next
omap <D-g> <D-g>
vmap <D-g> <D-g>
nmap <D-g> n
omap <D-f> <D-f>
vmap <D-f> <D-f>
nmap <D-f> /
omap <D-a> <D-a>
vmap <D-a> <D-a>
nmap <D-a> :if &slm != ""|exe ":norm gggHG"| else|exe ":norm ggVG"|endif
omap <D-z> <D-z>
vmap <D-z> <D-z>gv
nmap <D-z> u
omap <S-D-s> <D-s>
vmap <S-D-s> <D-s>gv
nmap <S-D-s> :confirm saveas =getcwd()/
omap <D-s> <D-s>
vmap <D-s> <D-s>gv
nmap <D-s> :if expand("%") == ""|browse confirm w| else|confirm w|endif
omap <D-w> <D-w>
vmap <D-w> <D-w>gv
nmap <D-w> :if winheight(2) < 0 |   confirm enew | else |   confirm close | endif
omap <D-o> <D-o>
vmap <D-o> <D-o>gv
nmap <D-o> :browse confirm e
omap <D-n> <D-n>
vmap <D-n> <D-n>gv
nmap <D-n> :confirm enew
map <xHome> <Home>
map <xEnd> <End>
map <S-xF4> <S-F4>
map <S-xF3> <S-F3>
map <S-xF2> <S-F2>
map <S-xF1> <S-F1>
map <xF4> <F4>
map <xF3> <F3>
map <xF2> <F2>
map <xF1> <F1>
vmap <BS> "-d
vmap <D-x> "+x
vmap <D-c> "+y
nmap <D-v> "+gP
imap <D-n> <D-n>
cmap <D-n> <D-n>
imap <D-o> <D-o>
cmap <D-o> <D-o>
imap <D-w> <D-w>
cmap <D-w> <D-w>
imap <D-s> <D-s>
cmap <D-s> <D-s>
imap <S-D-s> <D-s>
cmap <S-D-s> <D-s>
imap <D-z> <D-z>
cmap <D-z> <D-z>
cmap <D-c> 
map! <D-v> *
imap <D-a> <D-a>
cmap <D-a> <D-a>
imap <D-f> <D-f>
cmap <D-f> <D-f>
imap <D-g> <D-g>
cmap <D-g> <D-g>
let &cpo=s:cpo_save
unlet s:cpo_save
set autoindent
set autowrite
set backspace=2
set backup
set bufhidden=delete
set buftype=nofile
set cinkeys=0{,0},:,!^F,o,O,e
set cinoptions=>s,e0,n0,f0,{0,}0,^0,:s,=s,ps,t0,+s,(s,us,)20,*30,g0,+0,+0
set expandtab
set fileencodings=utf-8,latin1
if &filetype != 'vim'
set filetype=vim
endif
set formatoptions=tcql
set guioptions=agimrLt
set hidden
set history=50
set incsearch
set laststatus=2
set matchpairs=(:),{:},[:],<:>
set mouse=a
set report=0
set ruler
set scrolloff=3
set shiftwidth=4
set showmatch
set smartindent
set softtabstop=4
set nostartofline
set statusline=#%n:%<%F%m\ %a%=%([%{&ft}-File]%)%([%R]%)\ %l,%c%V\ %P
set suffixes=.bak,~,.o,.h,.info,.swp,.obj,.class
set noswapfile
if &syntax != 'java'
set syntax=java
endif
set ttyscroll=1
set viminfo='20,\"50
let s:so_save = &so | let s:siso_save = &siso | set so=0 siso=0
let v:this_session=expand("<sfile>:p")
silent only
cd ~/robocode/robots/wiki/nn
set shortmess=aoO
badd +628 ~/robocode/robots/pez/Marshmallow.java
badd +5 ~/robocode/robots/pez/Enemies.java
badd +241 ~/robocode/robots/pez/Enemy.java
badd +16 ~/robocode/robots/pez/Rutils.java
badd +49 ~/robocode/robots/pez/Driver.java
badd +7 ~/robocode/robots/pez/Marshmallow.properties
badd +1 ~/robocode/robots/pez/TuningFactor.java
badd +1 ~/robocode/robots/pez/CalculusList.java
badd +26 ~/robocode/robots/pez/mini/Gouldingi.java
badd +162 ~/robocode/robots/pez/mako/Mako.java
badd +3 ~/robocode/robots/sample/SittingDuck.java
badd +7 ~/robocode/robots/pez/nn/Orca.properties
badd +395 ~/robocode/robots/wiki/mako/MakoHT.java
badd +7 ~/robocode/robots/pez/mako/Mako.properties
badd +1 ~/robocode/robots/pez/mini/Gouldingi.properties
badd +1 OrcaM.java
badd +1 OrcaM.properties
args ~/robocode/robots/pez/Marshmallow.java
set splitbelow splitright
set nosplitbelow
set nosplitright
normal t
set winheight=1 winwidth=1
argglobal
edit OrcaM.java
setlocal autoindent
setlocal autoread
setlocal nobinary
setlocal bufhidden=
setlocal buflisted
setlocal buftype=
setlocal nocindent
setlocal cinkeys=0{,0},:,!^F,o,O,e
setlocal cinoptions=>s,e0,n0,f0,{0,}0,^0,:s,=s,ps,t0,+s,(s,us,)20,*30,g0,+0,+0
setlocal cinwords=if,else,while,do,for,switch
setlocal comments=s1:/*,mb:*,ex:*/,://,b:#,:%,:XCOMM,n:>,fb:-
setlocal commentstring=/*%s*/
setlocal complete=.,w,b,u,t,i
setlocal define=
setlocal dictionary=
setlocal nodiff
setlocal equalprg=
setlocal errorformat=
setlocal expandtab
if &filetype != 'java'
setlocal filetype=java
endif
setlocal foldcolumn=0
setlocal foldenable
setlocal foldexpr=0
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldmarker={{{,}}}
setlocal foldmethod=manual
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldtext=foldtext()
setlocal formatoptions=tcql
setlocal grepprg=
setlocal iminsert=0
setlocal imsearch=0
setlocal include=
setlocal includeexpr=
setlocal indentexpr=
setlocal indentkeys=0{,0},:,0#,!^F,o,O,e
setlocal noinfercase
setlocal iskeyword=@,48-57,_,192-255
setlocal keymap=
setlocal nolinebreak
setlocal nolisp
setlocal nolist
setlocal makeprg=
setlocal matchpairs=(:),{:},[:],<:>
setlocal modeline
setlocal modifiable
setlocal nrformats=octal,hex
setlocal nonumber
setlocal path=
setlocal nopreviewwindow
setlocal noreadonly
setlocal norightleft
setlocal noscrollbind
setlocal shiftwidth=4
setlocal noshortname
setlocal smartindent
setlocal softtabstop=4
setlocal suffixesadd=
setlocal noswapfile
if &syntax != 'java'
setlocal syntax=java
endif
setlocal tabstop=8
setlocal tags=
setlocal textwidth=0
setlocal thesaurus=
set nowrap
setlocal nowrap
setlocal wrapmargin=0
silent! normal zE
let s:l = 1 - ((0 * winheight(0) + 30) / 60)
if s:l < 1 | let s:l = 1 | endif
exe s:l
normal zt
1
normal 0
set winheight=1 winwidth=20 shortmess=filnxtToO
let s:sx = expand("<sfile>:p:r")."x.vim"
if file_readable(s:sx)
  exe "source " . s:sx
endif
let &so = s:so_save | let &siso = s:siso_save
