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
nmap <D-n> :confirm enew
vmap <D-n> <D-n>gv
omap <D-n> <D-n>
nmap <D-o> :browse confirm e
vmap <D-o> <D-o>gv
omap <D-o> <D-o>
nmap <D-w> :if winheight(2) < 0 |   confirm enew | else |   confirm close | endif
vmap <D-w> <D-w>gv
omap <D-w> <D-w>
nmap <D-s> :if expand("%") == ""|browse confirm w| else|confirm w|endif
vmap <D-s> <D-s>gv
omap <D-s> <D-s>
nmap <S-D-s> :confirm saveas =getcwd()/
vmap <S-D-s> <D-s>gv
omap <S-D-s> <D-s>
nmap <D-z> u
vmap <D-z> <D-z>gv
omap <D-z> <D-z>
nmap <D-a> :if &slm != ""|exe ":norm gggHG"| else|exe ":norm ggVG"|endif
vmap <D-a> <D-a>
omap <D-a> <D-a>
nmap <D-f> /
vmap <D-f> <D-f>
omap <D-f> <D-f>
nmap <D-g> n
vmap <D-g> <D-g>
omap <D-g> <D-g>
nmap <D-v> "+gP
vmap <D-v> "-d"*P
vmap <D-c> "+y
vmap <D-x> "+x
map Q gq
vnoremap p :let current_reg = @"gvdi=current_reg
map <F4> next
map <F1> previous
map <F9> :if exists("syntax_on") | syntax off | else | syntax on | endif 
omap <D-g> <D-g>
vmap <D-g> <D-g>
nmap <D-g> n
omap <D-f> <D-f>
vmap <D-f> <D-f>
nmap <D-f> /
omap <D-a> <D-a>
vmap <D-a> <D-a>
nmap <silent> <D-a> :if &slm != ""|exe ":norm gggHG"| else|exe ":norm ggVG"|endif
omap <D-z> <D-z>
vmap <D-z> <D-z>gv
nmap <D-z> u
omap <S-D-s> <D-s>
vmap <S-D-s> <D-s>gv
nmap <S-D-s> :browse confirm saveas
omap <D-s> <D-s>
vmap <D-s> <D-s>gv
nmap <silent> <D-s> :if expand("%") == ""|browse confirm w| else|confirm w|endif
omap <D-w> <D-w>
vmap <D-w> <D-w>gv
nmap <silent> <D-w> :if winheight(2) < 0 |   confirm enew | else |   confirm close | endif
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
cmap <D-g> <D-g>
imap <D-g> <D-g>
cmap <D-f> <D-f>
imap <D-f> <D-f>
cmap <D-a> <D-a>
imap <D-a> <D-a>
map! <D-v> *
cmap <D-c> 
cmap <D-z> <D-z>
imap <D-z> <D-z>
cmap <S-D-s> <D-s>
imap <S-D-s> <D-s>
cmap <D-s> <D-s>
imap <D-s> <D-s>
cmap <D-w> <D-w>
imap <D-w> <D-w>
cmap <D-o> <D-o>
imap <D-o> <D-o>
cmap <D-n> <D-n>
imap <D-n> <D-n>
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
set hlsearch
set iminsert=0
set imsearch=0
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
set termencoding=utf-8
set ttyscroll=1
set viminfo='20,\"50
let s:so_save = &so | let s:siso_save = &siso | set so=0 siso=0
let v:this_session=expand("<sfile>:p")
silent only
cd ~/robocode/robots/pez/nn
set shortmess=aoO
badd +26 ~/robocode/robots/pez/mini/Gouldingi.java
badd +71 ~/robocode/robots/pez/mako/Mako.java
badd +7 Orca.properties
badd +395 ~/robocode/robots/wiki/mako/MakoHT.java
badd +7 ~/robocode/robots/pez/mako/Mako.properties
badd +9 ~/robocode/robots/pez/mini/Gouldingi.properties
badd +1 ~/robocode/robots/wiki/nn/OrcaM.java
badd +1 ~/robocode/robots/wiki/nn/OrcaM.properties
badd +36 ~/Projects/src/mega/mega/java/pez/movement/RandomMovement.java
badd +16 ~/Projects/src/mega/mega/java/pez/movement/EscapeAreaMovement.java
badd +629 ~/Projects/src/mega/mega/java/pez/Marshmallow.java
badd +712 ~/Projects/src/mega/mega/java/pez/Enemy.java
badd +130 ~/Projects/src/mega/mega/java/pez/movement/MovementStrategy.java
badd +13 ~/Projects/src/mega/mega/java/pez/MarshmallowConstants.java
badd +8 ~/Projects/src/mega/mega/properties/Marshmallow.properties
badd +1 GB.java
args ~/robocode/robots/pez/Marshmallow.java
set splitbelow splitright
wincmd _ | wincmd |
split
1wincmd k
wincmd w
set nosplitbelow
set nosplitright
wincmd t
set winheight=1 winwidth=1
exe 'resize ' . ((&lines * 48 + 31) / 62)
wincmd w
exe 'resize ' . ((&lines * 11 + 31) / 62)
wincmd w
argglobal
edit GB.java
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
setlocal nowinfixheight
set nowrap
setlocal nowrap
setlocal wrapmargin=0
silent! normal! zE
let s:l = 1 - ((0 * winheight(0) + 24) / 48)
if s:l < 1 | let s:l = 1 | endif
exe s:l
normal! zt
1
normal! 0
wincmd w
argglobal
enew
setlocal autoindent
setlocal autoread
setlocal nobinary
setlocal bufhidden=delete
setlocal buflisted
setlocal buftype=quickfix
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
if &filetype != 'qf'
setlocal filetype=qf
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
setlocal nomodifiable
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
if &syntax != 'qf'
setlocal syntax=qf
endif
setlocal tabstop=8
setlocal tags=
setlocal textwidth=0
setlocal thesaurus=
setlocal winfixheight
set nowrap
setlocal nowrap
setlocal wrapmargin=0
silent! normal! zE
wincmd w
set winheight=1 winwidth=20 shortmess=filnxtToO
let s:sx = expand("<sfile>:p:r")."x.vim"
if file_readable(s:sx)
  exe "source " . s:sx
endif
let &so = s:so_save | let &siso = s:siso_save
